package com.nikodoko.importer;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import java.util.LinkedList;
import java.util.List;
import javax.lang.model.element.Name;

/** Simple representation of a class */
public class Class {
  // Example: com.util.java.List
  String path;
  // Example: List
  Name name;
  // Example: com
  Name tail;

  /**
   * The {@code Class} constructor.
   *
   * @param path its path
   * @param name its name
   */
  public Class(String path, Name name, Name tail) {
    this.path = path;
    this.name = name;
    this.tail = tail;
  }

  public Name name() {
    return name;
  }

  public String path() {
    return path;
  }

  public Name tail() {
    return tail;
  }

  /**
   * Parses a {@code Class} from a selector expression.
   *
   * <p>For example, something like java.util.List will have a path of java.util.List and a name of
   * List.
   *
   * <p>This is slightly hacky and relies heavily on type assertions, meaning it is highly coupled
   * with the actual JavacParser implementation.
   *
   * @param expr the expression to parse
   */
  public static Class fromSelectorExpr(JCExpression expr) {
    if (expr instanceof JCIdent) {
      Name name = ((JCIdent) expr).getName();
      return new Class("", name, name);
    }

    JCExpression selected = expr;
    List<String> fragments = new LinkedList<>();
    Name name = ((JCFieldAccess) selected).getIdentifier();

    while (!(selected instanceof JCIdent)) {
      fragments.add(((JCFieldAccess) selected).getIdentifier().toString());
      selected = ((JCFieldAccess) selected).getExpression();
    }

    Name tail = ((JCIdent) selected).getName();
    fragments.add(tail.toString());

    String path = String.join(".", Lists.reverse(fragments));

    return new Class(path, name, tail);
  }

  /** Debugging support. */
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", name).add("path", path).toString();
  }
}
