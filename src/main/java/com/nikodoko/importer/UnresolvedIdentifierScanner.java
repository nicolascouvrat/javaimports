package com.nikodoko.importer;

import com.google.common.collect.Sets;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import java.util.Set;
import java.util.function.BiFunction;
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
 *   <li>classes
 *   <li>for loops and other control structures
 *   <li>try-catch-finally (with resource or no)TODO: and more
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

  /**
   * Surround a visitXX function in a scope
   *
   * @param f the function to surround
   * @return a function with the same signature and the same return value
   */
  private <T> BiFunction<T, Void, Void> withScope(BiFunction<T, Void, Void> f) {
    return (T t, Void v) -> {
      openScope();
      Void r = f.apply(t, v);
      closeScope();
      return r;
    };
  }

  // The block case is a little special. Indeed, it will not only be called for "simple" blocks used
  // for scoping, but for any block. This means that, for example, visitMethod will create a scope,
  // then call visitBlock which will create another scope.
  //
  // While not perfectly accurate (this does not allow us to detect redeclaration of method
  // parameters in the method body for example), this is enough for our purpose of resolving
  // identifier, as any reference to the parameters in the block will be found in the immediately
  // enclosing (method) scope.
  //
  // For control structures that do not allow variable declaration outside of the block, like if,
  // then visitBlock will handle them nicely without the need to individually override visitIf
  @Override
  public Void visitBlock(BlockTree tree, Void v) {
    return withScope(super::visitBlock).apply(tree, v);
  }

  @Override
  public Void visitTry(TryTree tree, Void v) {
    return withScope(super::visitTry).apply(tree, v);
  }

  @Override
  public Void visitCatch(CatchTree tree, Void v) {
    return withScope(super::visitCatch).apply(tree, v);
  }

  // visitSwitch does not call visitBlock so has to be implemented separately
  @Override
  public Void visitSwitch(SwitchTree tree, Void v) {
    return withScope(super::visitSwitch).apply(tree, v);
  }

  @Override
  public Void visitForLoop(ForLoopTree tree, Void v) {
    // Make sure all variables get declared in the for loop scope, including the ones in the leading
    // parenthesis (aka the initializer)
    return withScope(super::visitForLoop).apply(tree, v);
  }

  @Override
  public Void visitEnhancedForLoop(EnhancedForLoopTree tree, Void v) {
    return withScope(super::visitEnhancedForLoop).apply(tree, v);
  }

  @Override
  public Void visitMethod(MethodTree tree, Void v) {
    // The function itself is declared in the parent scope, but its parameters will be declared in
    // the function's own scope
    declare(tree.getName());
    return withScope(super::visitMethod).apply(tree, v);
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
  public Void visitVariable(VariableTree tree, Void v) {
    declare(tree.getName());
    return super.visitVariable(tree, v);
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
