package com.nikodoko.javaimports.parser.internal;

import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Superclass;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Helper methods to convert {@code JCXxxx} classes to javaimports classes. */
public class JCHelper {
  static Superclass toSuperclass(JCExpression extendsClause) {
    List<String> reversedIdentifiers = new ArrayList<>();
    var pointer = extendsClause;
    while (!(pointer instanceof JCIdent)) {
      if (pointer instanceof JCTypeApply) {
        pointer = getGenericType(pointer);
        continue;
      }

      reversedIdentifiers.add(getFieldIdentifier(pointer));
      pointer = getFieldExpression(pointer);
    }

    reversedIdentifiers.add(getIdentifierName(pointer));
    Collections.reverse(reversedIdentifiers);
    var selector = Selector.of(reversedIdentifiers);
    return Superclass.unresolved(selector);
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
}
