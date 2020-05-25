package com.nikodoko.javaimports.parser.entities;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.parser.Scope;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.openjdk.tools.javac.tree.JCTree.JCExpression;
import org.openjdk.tools.javac.tree.JCTree.JCFieldAccess;
import org.openjdk.tools.javac.tree.JCTree.JCIdent;
import org.openjdk.tools.javac.tree.JCTree.JCTypeApply;

public class ScopedClassEntity implements Entity {
  private Scope scope;
  private ClassEntity entity;

  @Override
  public String name() {
    return entity.name();
  }

  @Nullable
  public Scope scope() {
    return scope;
  }

  @Override
  public Kind kind() {
    return entity.kind();
  }

  @Nullable
  public List<String> parentPath() {
    return entity.parentPath();
  }

  private ScopedClassEntity(ClassEntity entity) {
    this.entity = entity;
  }

  // FIXME: should be package private
  public static ScopedClassEntity of(ClassEntity entity) {
    return new ScopedClassEntity(entity);
  }

  // FIXME: get rid of me!
  public ClassEntity classEntity() {
    return entity;
  }

  /** Returns the {@code Entity}'s shallow copy. */
  public ScopedClassEntity clone() {
    ScopedClassEntity clone = new ScopedClassEntity(entity.clone());
    clone.scope = scope;
    return clone;
  }

  /** Set the extended class of this {@code Entity} */
  public void parentPath(List<String> path) {
    entity.parentPath(path);
  }

  /** Attach a scope to this {@code Entity} */
  public void attachScope(Scope scope) {
    this.scope = scope;
    entity.members(scope.entities().keySet());
  }

  /** Whether this {@code Entity} is extending anything */
  public boolean isChildClass() {
    return entity.isChildClass();
  }

  /**
   * Parses information about a class this entity extends from a selector expression.
   *
   * <p>For example, something like java.util.List will produce an parentPath of ["java", "util",
   * "List"]
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
    List<String> parentPath = new LinkedList<>();

    while (!(selected instanceof JCIdent)) {
      if (selected instanceof JCTypeApply) {
        // Ignore type parameters
        selected = (JCExpression) ((JCTypeApply) selected).getType();
        continue;
      }

      parentPath.add(((JCFieldAccess) selected).getIdentifier().toString());
      selected = ((JCFieldAccess) selected).getExpression();
    }

    parentPath.add(((JCIdent) selected).getName().toString());

    // We've built a reverse path, so reverse it and store it
    Collections.reverse(parentPath);
    entity.parentPath(parentPath);
  }

  public void extendWith(ScopedClassEntity parent) {
    Set<String> notYetResolved = new HashSet<>();
    for (String s : scope.notYetResolved()) {
      if (parent.scope().lookup(s) == null) {
        notYetResolved.add(s);
      }
    }

    this.scope.notYetResolved(notYetResolved);
  }

  /** Debugging support. */
  public String toString() {
    return MoreObjects.toStringHelper(this).add("scope", scope).add("entity", entity).toString();
  }
}
