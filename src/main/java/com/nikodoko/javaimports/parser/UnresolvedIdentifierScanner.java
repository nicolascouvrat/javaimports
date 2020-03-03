package com.nikodoko.javaimports.parser;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;

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
 * <p>It handles classes extending other classes differently, as they might be extended later.
 *
 * <p>Note that this will not consider any imports already present in the AST, meaning that all
 * identifiers referring to imported packages will be marked as unresolved (this is because {@link
 * com.sun.source.tree.ImportTree} does not contain the imported name).
 */
public class UnresolvedIdentifierScanner extends TreePathScanner<Void, Void> {
  private Scope topScope = new Scope(null);

  /** The top level scope of this scanner. */
  public Scope topScope() {
    return topScope;
  }
  // Copied from the original class where it is private
  private Void scanAndReduce(Iterable<? extends Tree> nodes, Void p, Void r) {
    return reduce(scan(nodes, p), r);
  }

  // Copied from the original class where it is private
  private Void scanAndReduce(Tree node, Void p, Void r) {
    return reduce(scan(node, p), r);
  }

  private void openScope() {
    topScope = new Scope(topScope);
  }

  // This assumes that classEntity has a kind of CLASS and an extended class path
  private Entity findParent(Entity classEntity) {
    List<String> parentPath = classEntity.extendedClassPath();

    // The parentPath may look like something like this: A.B.C
    // What we are going to do:
    //  - See if the leftmost part of the path is in topScope (A in our case)
    //  - If not we might find it later, so we return null
    //  - If yes we go down the path left to right, as long as we keep finding classes. If for
    //  whatever reason we do not (either because the class does not exist, or because it's not a
    //  class but something else), then we return a BAD Entity
    Entity maybeParent = topScope.lookup(parentPath.get(0));
    if (maybeParent == null) {
      return null;
    }

    Scope toScan = topScope;
    for (String s : parentPath) {
      maybeParent = toScan.lookup(s);
      if (maybeParent == null || maybeParent.kind() != Entity.Kind.CLASS) {
        // Whatever we are trying to extend, this is not going to work
        return new Entity(Entity.Kind.BAD);
      }
    }

    // If we got here, then we found it
    return maybeParent;
  }

  // This assumes that classEntity has a kind of CLASS and an extended class path
  private void tryToExtendClass(Entity classEntity) {
    if (classEntity.scope().notYetResolved().isEmpty()) {
      // No need to do anything
      return;
    }

    Entity parent = findParent(classEntity);
    if (parent == null) {
      topScope.parent().markAsNotYetExtended(classEntity);
      return;
    }

    if (parent.kind != Entity.Kind.CLASS) {
      // XXX: we could actually return a helpful error here, but instead just swallow all not yet
      // resolved identifiers, as this file will not compile anyway so let's not spend time trying
      // to resolve identifiers if we can avoid it.
      return;
    }

    // We found the parent, two cases:
    //  - the parent itself is not a child class, resolve what we can an pass the rest up
    //  - the parent itself is a child class, resolve what we can, switch the classEntity to be
    //  extending whatever the parent extends and try to resolve it again.
    if (!parent.isChildClass()) {
      // XXX: we could actually return useful errors here, such as "this is a private variable", but
      // let's not bother about it for now
      for (String s : classEntity.scope().notYetResolved()) {
        if (parent.scope().lookup(s) == null) {
          topScope.parent().markAsNotYetResolved(s);
        }
      }
      return;
    }

    Set<String> notYetResolved = new HashSet<>();
    for (String s : classEntity.scope().notYetResolved()) {
      if (parent.scope().lookup(s) == null) {
        notYetResolved.add(s);
      }
    }
    // For the resolution stage, everything will now happend as if classEntity was directly
    // extending the parent's parent instead of the parent. But we want the actuall classEntity
    // stored in the scope to still point towards the original parent, as someone else might extend
    // it.
    // We therefore clone the entity and use this clone to do further extensions. A shallow copy is
    // enough, as we will directly modify the extended path, and change nothing in the scope but the
    // unresolved identifiers (which do not matter in extension resolution).
    Entity clone = classEntity.clone();
    clone.extendedClassPath(parent.extendedClassPath());
    // this will also alter the "real" classEntity but it is fine.
    clone.scope().notYetResolved(notYetResolved);
    tryToExtendClass(clone);
  }

  private void closeScope(@Nullable Entity classEntity) {
    // First, try to find parents for all orphans child classes
    for (Entity childClass : topScope.notYetExtended()) {
      // We do not bubble identifiers in the case of orphan child classes, so manually go over them
      // trying to resolve
      Set<String> notYetResolved = new HashSet<>();
      for (String s : childClass.scope().notYetResolved()) {
        if (topScope.lookup(s) == null) {
          notYetResolved.add(s);
        }
      }
      childClass.scope().notYetResolved(notYetResolved);
      tryToExtendClass(childClass);
    }

    // Then, three scenarios:
    //  - we are closing a class, try again to resolve any not yet resolved identifiers (as they can
    //  be declared in any order in the class so we might have missed them) and bubble whatever is
    //  not found
    //  - we are closing a child class, do like for the class but do not bubble any left overs
    //  - we are not closing a class scope, bubble all not yet resolved identifiers up
    if (classEntity == null) {
      for (String s : topScope.notYetResolved()) {
        topScope.parent().markAsNotYetResolved(s);
      }
      topScope = topScope.parent();
      return;
    }

    if (!classEntity.isChildClass()) {
      for (String s : topScope.notYetResolved()) {
        if (!resolve(s)) {
          topScope.parent().markAsNotYetResolved(s);
        }
      }
      topScope = topScope.parent();
      return;
    }

    Set<String> notYetResolved = new HashSet<>();
    for (String s : topScope.notYetResolved()) {
      if (!resolve(s)) {
        notYetResolved.add(s);
      }
    }
    topScope.notYetResolved(notYetResolved);
    topScope = topScope.parent();
  }

  private void declare(String name, Entity entity) {
    topScope.insert(name, entity);
  }

  public Set<String> unresolved() {
    Set<String> unresolved = topScope.notYetResolved();
    for (Entity e : topScope.notYetExtended()) {
      unresolved.addAll(e.scope().notYetResolved());
    }

    return unresolved;
  }

  private boolean resolve(String identifier) {
    Scope current = topScope;
    while (current != null) {
      if (current.lookup(identifier) != null) {
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
      closeScope(null);
      return r;
    };
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree tree, Void v) {
    Void r = scan(tree.getPackageAnnotations(), v);
    // We do not want to generate any identifiers for the package nor the imports, so do not scan
    // them and go directly to the declarations
    r = scanAndReduce(tree.getTypeDecls(), v, r);
    return r;
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
  public Void visitAnnotation(AnnotationTree tree, Void v) {
    // Do not scan the annotation arguments, as something like
    // @Annotation(param="value")
    // Would produce a "param" identifier that we do not want
    Void r = scan(tree.getAnnotationType(), v);
    return r;
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

  // The visitMethod of TreeScanner, except it parses the type parameters first, so that the return
  // type does not produce an unresolved identifier if it is equal to that type parameter
  private Void visitMethodTypeParametersFirst(MethodTree tree, Void v) {
    Void r = scan(tree.getModifiers(), v);
    r = scanAndReduce(tree.getTypeParameters(), v, r);
    r = scanAndReduce(tree.getReturnType(), v, r);
    r = scanAndReduce(tree.getParameters(), v, r);
    r = scanAndReduce(tree.getReceiverParameter(), v, r);
    r = scanAndReduce(tree.getThrows(), v, r);
    r = scanAndReduce(tree.getBody(), v, r);
    r = scanAndReduce(tree.getDefaultValue(), v, r);
    return r;
  }

  @Override
  public Void visitMethod(MethodTree tree, Void v) {
    // The function itself is declared in the parent scope, but its parameters will be declared in
    // the function's own scope
    String name = tree.getName().toString();
    declare(name, new Entity(Entity.Kind.METHOD, name, tree.getModifiers()));
    return withScope(this::visitMethodTypeParametersFirst).apply(tree, v);
  }

  // Return true if the modifiers tree contains public or protected
  private boolean isExported(ModifiersTree tree) {
    return tree.getFlags().contains(Modifier.PUBLIC)
        || tree.getFlags().contains(Modifier.PROTECTED);
  }

  // Declares a class, returning the class entity
  private Entity declareNewClass(ClassTree tree) {
    String name = tree.getSimpleName().toString();
    Entity c = new Entity(Entity.Kind.CLASS, name, tree.getModifiers());
    declare(name, c);
    if (tree.getExtendsClause() != null) {
      c.registerExtendedClass((JCExpression) tree.getExtendsClause());
      topScope.markAsNotYetExtended(c);
    }
    return c;
  }

  @Override
  public Void visitTypeParameter(TypeParameterTree tree, Void v) {
    // A type parameter is like a variable, but for types, so declare it
    String name = tree.getName().toString();
    declare(name, new Entity(Entity.Kind.TYPE_PARAMETER, name));
    return super.visitTypeParameter(tree, v);
  }

  @Override
  public Void visitClass(ClassTree tree, Void v) {
    Entity newClass = declareNewClass(tree);
    openScope();

    // Do not scan the extends clause again, as we handle it separately and do not want to get
    // unresolved identifiers
    Void r = scan(tree.getModifiers(), v);
    r = scanAndReduce(tree.getTypeParameters(), v, r);
    r = scanAndReduce(tree.getImplementsClause(), v, r);
    r = scanAndReduce(tree.getMembers(), v, r);

    // Add the scope to the class entity before closing it, as we might need it later
    newClass.attachScope(topScope);
    closeScope(newClass);
    return r;
  }

  @Override
  public Void visitVariable(VariableTree tree, Void v) {
    String name = tree.getName().toString();
    declare(name, new Entity(Entity.Kind.VARIABLE, name, tree.getModifiers()));
    return super.visitVariable(tree, v);
  }

  @Override
  public Void visitIdentifier(IdentifierTree tree, Void unused) {
    // Try to resolve the identifier, if it fails add it to unresolved for the current scope
    String name = tree.getName().toString();
    if (!resolve(name)) {
      topScope.markAsNotYetResolved(name);
    }

    return null;
  }
}