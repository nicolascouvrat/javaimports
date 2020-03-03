package com.nikodoko.javaimports;

import com.google.common.base.MoreObjects;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCImport;

/** An {@code Import} represents a single import statement. */
public class Import {
  // Example: java.util
  String qualifier;
  // Example List
  String name;
  boolean isStatic;

  /**
   * An {@code Import} constructor.
   *
   * @param name its name
   * @param qualifier its qualifier
   * @param isStatic if it is static or not
   */
  public Import(String name, String qualifier, boolean isStatic) {
    this.name = name;
    this.qualifier = qualifier;
    this.isStatic = true;
  }

  /**
   * Create an {@code Import} from a {@link JCImport}.
   *
   * <p>This assumes that it is possible to convert a JCImport to a JCFieldAccess, which should be
   * possible looking at the implementation, but is not guaranteed.
   *
   * @param importTree the import object
   */
  public static Import fromJcImport(JCImport importTree) {
    String qualifier =
        ((JCFieldAccess) importTree.getQualifiedIdentifier()).getExpression().toString();
    String name = ((JCFieldAccess) importTree.getQualifiedIdentifier()).getIdentifier().toString();
    return new Import(name, qualifier, importTree.isStatic());
  }

  /** Debugging support. */
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("qualifier", qualifier)
        .add("isStatic", isStatic)
        .toString();
  }
}
