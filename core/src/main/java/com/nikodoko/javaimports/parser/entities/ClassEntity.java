package com.nikodoko.javaimports.parser.entities;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.parser.Scope;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import org.openjdk.tools.javac.tree.JCTree.JCExpression;
import org.openjdk.tools.javac.tree.JCTree.JCFieldAccess;
import org.openjdk.tools.javac.tree.JCTree.JCIdent;
import org.openjdk.tools.javac.tree.JCTree.JCTypeApply;

public class ClassEntity implements Entity {
  private Visibility visibility;
  private String name;
  private boolean isStatic;
  private Scope scope;
  private List<String> extendedClassPath;

  @Override
  public String name() {
    return name;
  }

  @Nullable
  public Scope scope() {
    return scope;
  }

  @Override
  public Kind kind() {
    return Kind.CLASS;
  }

  @Nullable
  public List<String> extendedClassPath() {
    return extendedClassPath;
  }

  ClassEntity(String name, Visibility visibility, boolean isStatic) {
    this.visibility = visibility;
    this.name = name;
    this.isStatic = isStatic;
  }

  /** Returns the {@code Entity}'s shallow copy. */
  public ClassEntity clone() {
    ClassEntity clone = new ClassEntity(name, visibility, isStatic);
    clone.scope = scope;
    clone.extendedClassPath = extendedClassPath;
    return clone;
  }

  /** Set the extended class of this {@code Entity} */
  public void extendedClassPath(List<String> path) {
    extendedClassPath = path;
  }

  /** Attach a scope to this {@code Entity} */
  public void attachScope(Scope scope) {
    this.scope = scope;
  }

  /** Whether this {@code Entity} is extending anything */
  public boolean isChildClass() {
    return extendedClassPath != null;
  }

  /**
   * Parses information about a class this entity extends from a selector expression.
   *
   * <p>For example, something like java.util.List will produce an extendedClassPath of ["java",
   * "util", "List"]
   *
   * <p>This is slightly hacky and relies heavily on type assertions, meaning it is highly coupled
   * with the actual JavacParser implementation.
   *
   * @param expr the expression to parse
   */
  public void registerExtendedClass(JCExpression expr) {
    JCExpression selected = expr;
    // The possible underlying types for selected should be: JCIdent (when we have a plain
    // identifier), JCFieldAccess (when it looks like A.B.C) or JCTypeApply when it is a
    // parametrized type like Package.Class<T, R>
    List<String> extendedClassPath = new LinkedList<>();

    while (!(selected instanceof JCIdent)) {
      if (selected instanceof JCTypeApply) {
        // Ignore type parameters
        selected = (JCExpression) ((JCTypeApply) selected).getType();
        continue;
      }

      extendedClassPath.add(((JCFieldAccess) selected).getIdentifier().toString());
      selected = ((JCFieldAccess) selected).getExpression();
    }

    extendedClassPath.add(((JCIdent) selected).getName().toString());

    // We've built a reverse path, so reverse it and store it
    Collections.reverse(extendedClassPath);
    this.extendedClassPath = extendedClassPath;
  }

  /** Debugging support. */
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("visibility", visibility)
        .add("isStatic", isStatic)
        .add("scope", scope)
        .add("extendedClassPath", extendedClassPath)
        .toString();
  }
}
