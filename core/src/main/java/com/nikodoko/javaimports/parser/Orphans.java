package com.nikodoko.javaimports.parser;

import com.nikodoko.javaimports.common.ClassDeclaration;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.OrphanClass;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Superclass;
import com.nikodoko.javaimports.common.Utils;
import com.nikodoko.javaimports.parser.internal.Scope;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** NOT thread safe! */
public interface Orphans {
  public interface Traverser {
    public ClassDeclaration next();

    public void addParent(ClassEntity parent);
  }

  public Traverser traverse();

  public Set<Identifier> unresolved();

  public boolean needsParents();

  public void addDeclarations(Set<Identifier> declarations);

  public static Orphans wrapping(Scope topScope) {
    return new OrphansImpl(topScope);
  }

  public static Orphans wrapping(Set<OrphanClass> classes) {
    return new DummyOrphansImpl(classes);
  }

  public static class DummyOrphansImpl implements Orphans {
    private Set<OrphanClass> classes;

    public class DummyTraverserImpl implements Traverser {
      private final Iterator<OrphanClass> classes;
      private final Set<OrphanClass> finalClasses;
      private OrphanClass current;

      public DummyTraverserImpl(Set<OrphanClass> classes) {
        this.classes = classes.iterator();
        this.finalClasses = new HashSet<>();
      }

      public ClassDeclaration next() {
        if (!classes.hasNext()) {
          traversalDone(finalClasses);
          return null;
        }

        var next = classes.next();
        current = next;
        return new ClassDeclaration(next.name, next.maybeParent);
      }

      public void addParent(ClassEntity parent) {
        if (parent == null) {
          finalClasses.add(current);
          return;
        }

        finalClasses.add(current.addParent(parent));
      }
    }

    public DummyOrphansImpl(Set<OrphanClass> classes) {
      this.classes = classes;
    }

    public Traverser traverse() {
      return new DummyTraverserImpl(classes);
    }

    public Set<Identifier> unresolved() {
      return classes.stream().flatMap(c -> c.unresolved.stream()).collect(Collectors.toSet());
    }

    void traversalDone(Set<OrphanClass> classes) {
      this.classes = classes;
    }

    public boolean needsParents() {
      return classes.stream().anyMatch(c -> c.maybeParent.isPresent());
    }

    public void addDeclarations(Set<Identifier> declarations) {
      var newClasses =
          classes.stream().map(o -> o.addDeclarations(declarations)).collect(Collectors.toSet());
      classes = newClasses;
    }
  }

  public static class OrphansImpl implements Orphans {
    private final Scope topScope;

    public OrphansImpl(Scope topScope) {
      Utils.checkNotNull(topScope);
      this.topScope = topScope;
    }

    public Traverser traverse() {
      return new TraverserImpl(topScope);
    }
    /**
     * Snapshots the current state by returning all identifiers currently unresolved.
     *
     * <p>Be sure to call this after each new traversal as it might have changed!
     */
    public Set<Identifier> unresolved() {
      Set<Identifier> collector = new HashSet<>();
      collectUnresolved(collector, topScope);
      return collector;
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

    /** Adds the provided declarations to all orphans. */
    public void addDeclarations(Set<Identifier> declarations) {
      declarations.forEach(topScope::declare);
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

    // TODO: should we add some sort of validation to check that we're not adding a random parent?
    public void addParent(ClassEntity parent) {
      // Should not be necessary
      if (parent == null) {
        return;
      }
      enrich(current, parent);
    }
  }

  private static void collectUnresolved(Set<Identifier> collector, Scope scope) {
    collector.addAll(scope.unresolved);
    for (var s : scope.childScopes) {
      collectUnresolved(collector, s);
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

  private static void enrich(Scope child, ClassEntity parent) {
    child.maybeClass = child.maybeClass.map(c -> c.addParent(parent.maybeParent));
    parent.declarations.forEach(child::declare);
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
