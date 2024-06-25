package com.nikodoko.javaimports.parser.internal;

import com.nikodoko.javaimports.common.ClassDeclaration;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Superclass;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
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
  private Scope topScope = new Scope();
  private boolean isRootClass = false;

  /** The top level scope of this scanner. */
  public Scope topScope() {
    return topScope;
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
    // When the annotation arguments contain assignments (like param="value"), do not scan the left
    // hand side as it would produce identifiers we do not want.
    var filteredArguments =
        tree.getArguments().stream()
            .map(expr -> expr instanceof JCAssign ? ((JCAssign) expr).getExpression() : expr)
            .collect(Collectors.toList());

    Void r = scan(tree.getAnnotationType(), v);
    r = scanAndReduce(filteredArguments, v, r);
    return r;
  }

  @Override
  public Void visitCatch(CatchTree tree, Void v) {
    return withScope(super::visitCatch).apply(tree, v);
  }

  // Because "The scope of an enum constant C declared in an enum type T is the body of T, and
  // any case label of a switch statement whose expression is of enum type T" (see
  // https://docs.oracle.com/javase/specs/jls/se8/html/jls-6.html#jls-6.3), we should ignore
  // unknown symbols in case labels to avoid importing things we don't want to import.
  // This is not ideal as there could be cases where we want to import whatever is used as label,
  // but it's better to be conservative here (is this case, the import will have to be added by
  // hand).
  @Override
  public Void visitCase(CaseTree tree, Void v) {
    return scan(((JCCase) tree).stats, v);
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
    declare(new Identifier(name));
    return withScope(this::visitMethodTypeParametersFirst).apply(tree, v);
  }

  // Return true if the modifiers tree contains public or protected
  private boolean isExported(ModifiersTree tree) {
    return tree.getFlags().contains(Modifier.PUBLIC)
        || tree.getFlags().contains(Modifier.PROTECTED);
  }

  @Override
  public Void visitTypeParameter(TypeParameterTree tree, Void v) {
    // A type parameter is like a variable, but for types, so declare it
    String name = tree.getName().toString();
    declare(new Identifier(name));
    return super.visitTypeParameter(tree, v);
  }

  @Override
  public Void visitNewClass(NewClassTree tree, Void v) {
    Void r = scan(tree.getEnclosingExpression(), v);
    r = scanAndReduce(tree.getIdentifier(), v, r);
    r = scanAndReduce(tree.getTypeArguments(), v, r);
    r = scanAndReduce(tree.getArguments(), v, r);
    if (tree.getClassBody() != null) {
      // We are declaring an anonymous class that we want to treat as a class extending the class we
      // are instanciating.
      var c = createClassEntity(null, tree.getClassBody(), (JCExpression) tree.getIdentifier());
      visitClass(tree.getClassBody(), v, c);
    }

    return r;
  }

  @Override
  public Void visitClass(ClassTree tree, Void v) {
    var className = new Identifier(tree.getSimpleName().toString());
    declare(className);
    var c = createClassEntity(className, tree, (JCExpression) tree.getExtendsClause());
    return visitClass(tree, v, c);
  }

  private Void visitClass(ClassTree tree, Void v, ClassEntity newClass) {
    openClassScope(newClass);

    // Do not scan the extends clause again, as we handle it separately and do not want to get
    // unresolved identifiers
    Void r = scan(tree.getModifiers(), v);
    r = scanAndReduce(tree.getTypeParameters(), v, r);
    r = scanAndReduce(tree.getImplementsClause(), v, r);
    r = scanAndReduce(tree.getMembers(), v, r);

    closeClassScope(newClass);
    return r;
  }

  private ClassEntity createClassEntity(
      Identifier name, ClassTree tree, JCExpression extendsClause) {
    var builder = name == null ? ClassEntity.anonymous() : ClassEntity.named(Selector.of(name));
    var isEnum = tree.getKind() == Tree.Kind.ENUM;
    if (isEnum && extendsClause == null) {
      return builder
          .extending(Superclass.resolved(new Import(Selector.of("java", "lang", "Enum"), false)))
          .build();
    }

    if (extendsClause == null) {
      return builder.build();
    }

    return builder.extending(JCHelper.toSuperclass(extendsClause)).build();
  }

  private void openClassScope(ClassEntity entity) {
    openScope();
    topScope.maybeClass = Optional.of(new ClassDeclaration(entity.name, entity.maybeParent));
  }

  private void closeClassScope(ClassEntity classEntity) {
    closeScope();
  }

  @Override
  public Void visitVariable(VariableTree tree, Void v) {
    String name = tree.getName().toString();
    declare(new Identifier(name));
    return super.visitVariable(tree, v);
  }

  @Override
  public Void visitIdentifier(IdentifierTree tree, Void unused) {
    // Try to resolve the identifier, if it fails add it to unresolved for the current scope
    String name = tree.getName().toString();
    var identifier = new Identifier(name);
    topScope.maybeAddUnresolved(identifier);

    return null;
  }

  private void declare(Identifier identifier) {
    topScope.identifiers.add(identifier);
    topScope.declare(identifier);
  }

  /**
   * Surround a visitXX function in a scope
   *
   * @param f the function to surround
   * @return a function with the same signature and the same return value
   */
  private <T> BiFunction<T, Void, Void> withScope(BiFunction<T, Void, Void> f) {
    return (T t, Void v) -> {
      openNonClassScope();

      Void r = f.apply(t, v);
      closeNonClassScope();
      return r;
    };
  }

  private void openNonClassScope() {
    openScope();
  }

  private void openScope() {
    Scope newScope = new Scope();
    newScope.parent = topScope;
    topScope.addChild(newScope);
    topScope = newScope;
  }

  private void closeNonClassScope() {
    closeScope();
  }

  private void closeScope() {
    moveUpInHierarchy();
    topScope = topScope.parent;
  }

  private void moveUpInHierarchy() {}

  // Copied from the original class where it is private
  private Void scanAndReduce(Iterable<? extends Tree> nodes, Void p, Void r) {
    return reduce(scan(nodes, p), r);
  }

  // Copied from the original class where it is private
  private Void scanAndReduce(Tree node, Void p, Void r) {
    return reduce(scan(node, p), r);
  }
}
