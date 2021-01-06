package com.nikodoko.javaimports.parser;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.common.Selector;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCImport;
import java.util.ArrayList;
import java.util.Objects;

/** An {@code Import} represents a single import statement. */
public class Import {
  // Example: java.util
  final String qualifier;
  // Example List
  final String name;
  final boolean isStatic;

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
    this.isStatic = isStatic;
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

  public String name() {
    return name;
  }

  public String qualifier() {
    return qualifier;
  }

  public int pathLength() {
    return qualifier.split("\\.").length;
  }

  public boolean isInJavaUtil() {
    return qualifier.equals("java.util");
  }

  // TODO: remove
  public com.nikodoko.javaimports.common.Import toNew() {
    var identifiers = new ArrayList<String>();
    var fragments = qualifier.split("\\.");
    for (var frag : fragments) {
      identifiers.add(frag);
    }
    identifiers.add(name);
    return new com.nikodoko.javaimports.common.Import(Selector.of(identifiers), isStatic);
  }

  public static Import fromNew(com.nikodoko.javaimports.common.Import i) {
    var str = i.selector.toString();
    var cutoff = str.lastIndexOf(".");
    var qualifier = str.substring(0, cutoff);
    var name = str.substring(cutoff + 1);
    return new Import(name, qualifier, i.isStatic);
  }

  /** Creates a fully qualified import statement from this {@code Import} object. */
  public String asStatement() {
    return String.format("import%s %s.%s;", isStatic ? " static" : "", qualifier, name);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof Import)) {
      return false;
    }

    Import other = (Import) o;
    return Objects.equals(qualifier, other.qualifier)
        && Objects.equals(name, other.name)
        && isStatic == other.isStatic;
  }

  @Override
  public int hashCode() {
    return Objects.hash(qualifier, name, isStatic);
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
