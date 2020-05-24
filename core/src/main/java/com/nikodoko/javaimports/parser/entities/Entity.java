package com.nikodoko.javaimports.parser.entities;

import com.nikodoko.javaimports.parser.Scope;
import java.util.List;
import javax.annotation.Nullable;
import org.openjdk.tools.javac.tree.JCTree.JCExpression;

/**
 * An {@code Entity} describes a named language entity such as a method (static or no), class or
 * variable. It always comes with a {@link Kind} and a {@link Visibility}, and might contain other
 * information such as a name, or if it is static or not.
 *
 * <p>In addition, a {@link Kind#CLASS} {@code Entity} can contain its {@link Scope} and information
 * regarding the class it extends (if any).
 */
public interface Entity {
  /** Returns the {@code Entity}'s shallow copy. */
  public Entity clone();

  /** An {@code Entity}'s declared name */
  public String name();

  /** The {@link Scope} attached to this {@code Entity} */
  @Nullable
  public Scope scope();

  /** The kind of this {@code Entity} */
  public Kind kind();

  /** The path of the extended class of this {@code Entity} */
  @Nullable
  public List<String> extendedClassPath();

  /** Set the extended class of this {@code Entity} */
  public void extendedClassPath(List<String> path);

  /** Attach a scope to this {@code Entity} */
  public void attachScope(Scope scope);

  /** Whether this {@code Entity} is extending anything */
  public boolean isChildClass();

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
  public void registerExtendedClass(JCExpression expr);
}
