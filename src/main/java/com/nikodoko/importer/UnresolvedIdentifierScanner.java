package com.nikodoko.importer;

import com.google.common.collect.Sets;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Name;

/**
 * Visits an AST, recording all identifiers that cannot be resolved in the current file (either
 * because they are in the universe scope, the package scope or because they have to be imported).
 *
 * <p>This works by opening and closing scopes whenever the Java language defines one:
 *
 * <ul>
 *   <li>methods
 *   <li>classes TODO: add more!
 * </ul>
 *
 * Everytime an Identifier is encountered, it tries to resolve it by looking it up in all scopes up
 * to the outermost one (encapsulating the whole AST). If it cannot be found, it is then added to an
 * unresolved set.
 *
 * <p>Note that this will not consider any imports already present in the AST, meaning that all
 * identifiers referring to imported packages will be marked as unresolved (this is because {@link
 * com.sun.source.tree.ImportTree} does not contain the imported name). Similarly, it will also
 * contain a "trash" identifier that is in fact the package root ({@code package com.example;} will
 * produce {@code "com"}).
 */
public class UnresolvedIdentifierScanner extends TreePathScanner<Void, Void> {
  // We need a concurrent set here, as scanning is done asynchronously and we might want to remove
  // elements from it while processing the tree.
  private final Set<Name> unresolved = Sets.newConcurrentHashSet();
  private Scope topScope = new Scope(null);

  private void openScope() {
    topScope = new Scope(topScope);
  }

  private void closeScope() {
    System.out.println("Scope contained: " + topScope.toString());
    topScope = topScope.parent();
  }

  private void declare(Name... identifiers) {
    for (Name i : identifiers) {
      topScope.insert(i);
    }
  }

  public Set<String> unresolved() {
    return unresolved.stream().map(Name::toString).collect(Collectors.toSet());
  }

  private boolean resolve(Name identifier) {
    Scope current = topScope;
    while (current != null) {
      if (current.lookup(identifier)) {
        return true;
      }
      current = current.parent();
    }

    return false;
  }

  @Override
  public Void visitBlock(BlockTree tree, Void v) {
    return super.visitBlock(tree, v);
  }

  @Override
  public Void visitMethod(MethodTree tree, Void v) {
    // The function itself is declared in the parent scope, but its parameters will be declared in
    // the function's own scope
    declare(tree.getName());
    openScope();
    // We don't need to declare the parameters now, as visitVariable will be called for us
    Void r = super.visitMethod(tree, v);
    // We're done visiting all child nodes, terminate the scope as it will be unreachable anyway.
    closeScope();
    return r;
  }

  @Override
  public Void visitVariable(VariableTree tree, Void v) {
    declare(tree.getName());
    return super.visitVariable(tree, v);
  }

  @Override
  public Void visitClass(ClassTree tree, Void v) {
    // Declare the class name in the current scope before opening a new one
    declare(tree.getSimpleName());
    openScope();
    Void r = super.visitClass(tree, v);
    // We resolve as we go, but scoping for a class works a little differently, as it ignores the
    // order in which methods (or variables) are declared. We are about to close the scope, so we've
    // collected all existing identifiers for this class: try one last time to resolve them.
    for (Name u : unresolved) {
      if (resolve(u)) {
        unresolved.remove(u);
      }
    }
    closeScope();
    return r;
  }

  @Override
  public Void visitIdentifier(IdentifierTree tree, Void unused) {
    // Try to resolve the identifier, if it fails add it to unresolved.
    if (!resolve(tree.getName())) {
      unresolved.add(tree.getName());
    }

    return null;
  }
}
