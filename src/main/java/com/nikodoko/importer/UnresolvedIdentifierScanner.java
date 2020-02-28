package com.nikodoko.importer;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;
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
 *   <li>try-catch-finally (with resource or no)
 *   <li>lambdas
 *   <li>and all blocks in general
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
  private Scope topScope = new Scope(null);
  private Map<Name, Scope> classScopes = new HashMap<>();

  private void openScope(
      boolean isClassScope, @Nullable Name className, @Nullable Tree extendsClause) {
    Scope newScope = new Scope(topScope);

    // Opening a scope works a little differently for a class. Indeed, a class can extend another
    // one, and that other one can be declared elsewhere in the file. In this case, we want to be
    // able to later resolve the child class unresolved identifiers using the entities declared in
    // the parent class.
    //
    // For this purpose, register class scope to a different set too
    if (isClassScope) {
      if (extendsClause != null) {
        Class extendedClass = Class.fromSelectorExpr((JCExpression) extendsClause);
        newScope.registerExtendedClass(extendedClass);

        // In this case, we need to declare a trash identifier corresponding to the tail, as it will
        // be visited as an identifier
        newScope.insert(extendedClass.tail());
      }

      classScopes.put(className, newScope);
    }

    topScope = newScope;
  }

  private void closeScope(boolean isClassScope) {
    System.out.println("Scope contained: " + topScope.toString());

    for (Name n : topScope.unresolved()) {
      // If we are not closing a class scope, then we might still resolve identifiers at the class
      // level (as declaration order does not matter in a class, we can discover these identifiers
      // later)
      if (!isClassScope) {
        topScope.parent().markAsUnresolved(n);
        continue;
      }
      // We resolve as we go, but scoping for a class works a little differently, as it ignores the
      // order in which methods (or variables) are declared. We are about to close the scope, so
      // we've
      // collected all existing identifiers for this class: try one last time to resolve them.
      //
      // In the case of a class, we do not bubble up the unresolved identifiers as we still need to
      // try to resolve them using extends clauses, and this will be done later
      if (resolve(n)) topScope.resolve(n);
    }

    topScope = topScope.parent();
  }

  private void declare(boolean isExported, Name identifier) {
    topScope.insert(identifier);
    if (isExported) topScope.insertExported(identifier);
  }

  public Set<String> unresolved() {
    Set<String> unresolved = new HashSet<>();
    for (Name n : topScope.unresolved()) {
      unresolved.add(n.toString());
    }

    for (Scope s : classScopes.values()) {
      for (Name n : s.unresolved()) {
        // Check if the class being extended is in the same file, and if it is try to lookup the
        // unresolved identifier one more time.
        //
        // We can't resolve it if it has a path anyway, as it means it has not been declared in
        // the same file.
        if (s.hasExtends() && s.extendedClass().path() == "") {
          Scope extendedScope = classScopes.get(s.extendedClass().name());
          if (extendedScope != null) {
            if (extendedScope.lookupExported(n)) continue;
          }
        }

        unresolved.add(n.toString());
      }
    }

    return unresolved;
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
      openScope(false, null, null);
      Void r = f.apply(t, v);
      closeScope(false);
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
  public Void visitLambdaExpression(LambdaExpressionTree tree, Void v) {
    return withScope(super::visitLambdaExpression).apply(tree, v);
  }

  @Override
  public Void visitMethod(MethodTree tree, Void v) {
    // The function itself is declared in the parent scope, but its parameters will be declared in
    // the function's own scope
    declare(isExported(tree.getModifiers()), tree.getName());
    return withScope(super::visitMethod).apply(tree, v);
  }

  // Return true if the modifiers tree contains public or protected
  private boolean isExported(ModifiersTree tree) {
    return tree.getFlags().contains(Modifier.PUBLIC)
        || tree.getFlags().contains(Modifier.PROTECTED);
  }

  @Override
  public Void visitClass(ClassTree tree, Void v) {
    // Declare the class name in the current scope before opening a new one
    declare(isExported(tree.getModifiers()), tree.getSimpleName());
    openScope(true, tree.getSimpleName(), tree.getExtendsClause());
    Void r = super.visitClass(tree, v);
    closeScope(true);
    return r;
  }

  @Override
  public Void visitVariable(VariableTree tree, Void v) {
    declare(isExported(tree.getModifiers()), tree.getName());
    return super.visitVariable(tree, v);
  }

  @Override
  public Void visitIdentifier(IdentifierTree tree, Void unused) {
    // Try to resolve the identifier, if it fails add it to unresolved for the current scope
    if (!resolve(tree.getName())) {
      topScope.markAsUnresolved(tree.getName());
    }

    return null;
  }
}
