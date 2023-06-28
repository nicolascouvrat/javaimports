package com.nikodoko.javaimports.parser;

import com.nikodoko.javaimports.common.ClassDeclaration;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Superclass;
import com.nikodoko.javaimports.common.Utils;
import com.nikodoko.javaimports.parser.internal.Scope;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Wraps the top {@code Scope} of a {@link ParsedFile} and exposes its orphan classes so that
 * external parents can be found.
 *
 * <p>Note that it will try to prioritise finding local parents, and that it will also hide classes
 * that are not safe to extend now. For example, if an orphan is declared inside another orphan,
 * traversing through this {@code Orphans} will NOT expose the inner orphan, as it is not certain at
 * this point that the parent of the outer orphan does not provide the parent of the inner orphan as
 * well. Once the outer orphan will have found its parent, doing a new traversal on the same {@code
 * Orphans} will expose the inner orphan.
 */
public interface Orphans {
  public interface Traverser {
    /**
     * Returns a {@link ClassDeclaration} corresponding to the next orphan, or {@code null} if there
     * are no more orphans currently available.
     *
     * <p>Note that {@code next()} returning null does not mean that there are no more orphans at
     * all. It is possible that initiating a new traversal reveals new ones, or that some are hidden
     * until an external parent is added. To know if there are still orphans in need or parents,
     * call {@link Orphans#needsParents}.
     */
    public ClassDeclaration next();

    /**
     * Adds a parent to the last orphan class returned by this {@code Traverser}.
     *
     * <p>It is the caller's responsibility to ensure that the provided {@code parent} is indeed a
     * parent of that orphan.
     */
    public void addParent(Import i, ClassEntity parent);
  }

  /** Initiates an iteration of orphan classes in the underlying scope. */
  public Traverser traverse();

  /**
   * Returns {@code true} if there are still orphans in need of parents in the underlying scope.
   *
   * <p>Note that this will also match orphans that are currently hidden by the iteration.
   */
  public boolean needsParents();

  public static Orphans wrapping(Scope topScope) {
    return new OrphansImpl(topScope);
  }

  /** NOT thread safe! */
  public static class OrphansImpl implements Orphans {
    private final Scope topScope;

    public OrphansImpl(Scope topScope) {
      Utils.checkNotNull(topScope);
      this.topScope = topScope;
    }

    public Traverser traverse() {
      return new TraverserImpl(topScope);
    }

    /** Returns true if there are still any class scopes in need of parents. */
    public boolean needsParents() {
      return needsParents(topScope);
    }

    private static boolean needsParents(Scope s) {
      if (hasSuperclass(s)) {
        return true;
      }

      return s.childScopes.stream().anyMatch(OrphansImpl::needsParents);
    }
  }

  public static class TraverserImpl implements Traverser {
    private final Deque<Scope> queue;
    private Scope current;

    private TraverserImpl(Scope topScope) {
      this.queue = new ArrayDeque<>();
      this.current = null;
      queue.add(topScope);
    }

    public ClassDeclaration next() {
      current = queue.poll();
      while (current != null && !hasSuperclass(current)) {
        queue.addAll(current.childScopes);
        current = queue.poll();
      }

      if (current == null) {
        return null;
      }

      var hasLocalParent = findLocalParents(current);
      if (hasSuperclass(current)) {
        if (hasLocalParent) {
          // We cannot look at the child of this scope because it is not resolved, but we know that
          // its parent can be found locally (which will take priority over everything else), so we
          // do not expose it
          return next();
        }

        // We found an orphan class
        return new ClassDeclaration(Orphans.getClass(current).name(), getSuperclass(current));
      }

      // We resolved it locally, move on
      queue.addAll(current.childScopes);
      return next();
    }

    @Override
    public void addParent(Import parentImport, ClassEntity parent) {
      enrich(current, parent, parentImport);
    }
  }

  private static boolean findLocalParents(Scope scope) {
    if (!hasSuperclass(scope)) {
      return false;
    }

    var superclass = getSuperclass(scope);
    if (superclass.isResolved()) {
      return false;
    }

    var selector = superclass.getUnresolved();
    var parentScope = scope.parent;
    Optional<ParentClassScope> maybeParentClassScope = Optional.empty();
    while (parentScope != null && maybeParentClassScope.isEmpty()) {
      maybeParentClassScope = findClassScope(parentScope, selector, true, scope);
      parentScope = parentScope.parent;
    }

    if (maybeParentClassScope.isEmpty()) {
      return false;
    }

    var parentClassScope = maybeParentClassScope.get();
    // This means a class on the way to the parent class is an orphan, we cannot use it yet, but we
    // know it's there and don't want to spend time looking for it elsewhere
    if (!parentClassScope.usable()) {
      return true;
    }

    var hasLocalParents = findLocalParents(parentClassScope.scope());
    enrich(scope, parentClassScope.scope());
    return hasLocalParents;
  }

  private static void enrich(Scope child, ClassEntity parent, Import parentImport) {
    child.maybeClass = child.maybeClass.map(c -> c.addParent(parent.maybeParent));
    parent.declarations.forEach(child::declare);

    for (var s : child.childScopes) {
      resolveSuperclasses(s, parent, parentImport);
    }
  }

  private static void resolveSuperclasses(Scope scope, ClassEntity parent, Import parentImport) {
    for (var s : scope.childScopes) {
      resolveSuperclasses(s, parent, parentImport);
    }

    if (hasSuperclass(scope)) {
      var superclass = getSuperclass(scope);
      if (!superclass.isResolved()
          && parent.declarations.stream().anyMatch(superclass.getUnresolved()::startsWith)) {
        var resolved = new Import(parentImport.selector.combine(superclass.getUnresolved()), false);
        var decl = new ClassDeclaration(getClass(scope).name(), Superclass.resolved(resolved));
        scope.maybeClass = Optional.of(decl);
      }
    }
  }

  private static void enrich(Scope child, Scope parent) {
    parent.declarations.forEach(child::declare);
    child.maybeClass = child.maybeClass.map(c -> c.addParent(getClass(parent).maybeParent()));
  }

  public record ParentClassScope(Scope scope, boolean usable) {}

  private static Optional<ParentClassScope> findClassScope(
      Scope scope, Selector selector, boolean usable, Scope from) {
    var maybeCandidate =
        scope.childScopes.stream()
            .filter(s -> s.maybeClass.map(c -> selector.startsWith(c.name())).orElse(false))
            .findFirst();
    if (maybeCandidate.isEmpty()) {
      return Optional.empty();
    }

    var candidate = maybeCandidate.get();
    if (candidate == from) {
      throw new RuntimeException("Cyclic inheritence detected around " + getClass(candidate));
    }

    if (selector.equals(getClass(candidate).name())) {
      return Optional.of(new ParentClassScope(candidate, usable));
    }

    // This scope is potentially on the path to the requested parent. If it is an orphan itself, and
    // we cannot find parents for it locally, then we want to keep in mind that even if we find a
    // parent it is not usable as is (as it might provide additional identifiers we don't know about
    // yet).
    if (hasSuperclass(candidate)) {
      findLocalParents(candidate);
    }

    return findClassScope(
        candidate, selector.rebase(getClass(candidate).name()), !hasSuperclass(candidate), from);
  }

  private static boolean hasSuperclass(Scope scope) {
    return scope.maybeClass.isPresent() && scope.maybeClass.get().maybeParent().isPresent();
  }

  private static ClassDeclaration getClass(Scope scope) {
    if (scope.maybeClass.isEmpty()) {
      throw new IllegalArgumentException("This scope does not correspond to a class");
    }

    return scope.maybeClass.get();
  }

  private static Superclass getSuperclass(Scope scope) {
    if (!hasSuperclass(scope)) {
      throw new IllegalArgumentException("This scope does not have a superclass");
    }

    return scope.maybeClass.get().maybeParent().get();
  }
}
