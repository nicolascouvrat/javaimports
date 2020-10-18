package com.nikodoko.javaimports.parser.internal;

import java.util.Optional;
import org.openjdk.tools.javac.tree.JCTree.JCExpression;
import org.openjdk.tools.javac.tree.JCTree.JCFieldAccess;
import org.openjdk.tools.javac.tree.JCTree.JCIdent;
import org.openjdk.tools.javac.tree.JCTree.JCTypeApply;

/** Utility methods for {@link ClassSelector}. */
public class ClassSelectors {
  /** Returns a {@code ClassSelector} representing the path given by first.others. */
  public static ClassSelector of(String first, String... others) {
    Selector head = new Selector(first);
    Selector tail = head;
    for (String other : others) {
      tail = updateTail(tail, other);
    }

    return head;
  }

  private static Selector updateTail(Selector previous, String next) {
    Selector newTail = new Selector(next);
    previous.next = newTail;
    return newTail;
  }

  /** Returns a {@code ClassSelector} representing the given {@code extendsClause}. */
  public static ClassSelector of(JCExpression extendsClause) {
    JCExpression selector = extendsClause;
    Selector head = null;
    while (!(selector instanceof JCIdent)) {
      if (selector instanceof JCTypeApply) {
        selector = getGenericType(selector);
        continue;
      }

      head = updateHead(head, getFieldIdentifier(selector));
      selector = getFieldExpression(selector);
    }

    return updateHead(head, getIdentifierName(selector));
  }

  private static Selector updateHead(Selector previous, String next) {
    Selector newHead = new Selector(next);
    newHead.next = previous;
    return newHead;
  }

  private static JCExpression getGenericType(JCExpression parametrized) {
    return (JCExpression) ((JCTypeApply) parametrized).getType();
  }

  private static String getFieldIdentifier(JCExpression field) {
    return ((JCFieldAccess) field).getIdentifier().toString();
  }

  private static JCExpression getFieldExpression(JCExpression field) {
    return ((JCFieldAccess) field).getExpression();
  }

  private static String getIdentifierName(JCExpression identifier) {
    return ((JCIdent) identifier).getName().toString();
  }

  static class Selector implements ClassSelector {
    String selector;
    ClassSelector next;

    Selector(String selector) {
      this.selector = selector;
    }

    @Override
    public String selector() {
      return selector;
    }

    @Override
    public Optional<ClassSelector> next() {
      return Optional.ofNullable(next);
    }

    @Override
    public String toString() {
      if (next == null) {
        return selector;
      }

      return String.format("%s.%s", selector, next);
    }
  }
}
