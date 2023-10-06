package com.nikodoko.javaimports.parser.internal;

import com.google.common.collect.Range;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Superclass;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** Helper methods to convert {@code JCXxxx} classes to javaimports classes. */
public class JCHelper {
  static Superclass toSuperclass(JCExpression extendsClause) {
    var selector = toSelector(extendsClause);
    // Hack: sometimes, the extend clause can represent a resolved superclass
    // We cannot know it for sure at this stage, but what we can do is assume that if the selector
    // does not start with a capital letter, then it starts with a package name and is therefore
    // resolved
    if (Character.isLowerCase(selector.toString().charAt(0))) {
      return Superclass.resolved(new Import(selector, false));
    }

    return Superclass.unresolved(selector);
  }

  public static Selector toSelector(JCExpression expr) {
    List<String> reversedIdentifiers = new ArrayList<>();
    var pointer = expr;
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
    return Selector.of(reversedIdentifiers);
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

  public static Import toImport(JCImport importTree) {
    var selector = toSelector((JCExpression) importTree.getQualifiedIdentifier());
    return new Import(selector, importTree.isStatic());
  }

  public static ParsedFile.Builder toParsedFileBuilder(JCCompilationUnit unit) {
    var pkg = JCHelper.toSelector((JCExpression) unit.getPackageName());
    var packageEndPos = findEndOfPackageClause(unit);
    var builder = ParsedFile.inPackage(pkg, packageEndPos);
    // unit.getImports() potentially contains the same import multiple times, in which case we want
    // to consider only one of them (and mark the others as duplicates)
    var imports = new HashSet<Import>();
    for (var existingImport : unit.getImports()) {
      var i = JCHelper.toImport(existingImport);
      if (imports.contains(i)) {
        builder.addDuplicateImportPosition(rangeOf(unit, existingImport));
        continue;
      }

      imports.add(i);
      builder.addImport(i);
    }

    return builder;
  }

  // TODO: clean this up
  private static int findEndOfPackageClause(JCCompilationUnit unit) {
    var pkg = (JCExpression) unit.getPackageName();
    return pkg.getEndPosition(unit.endPositions);
  }

  // TODO: clean this up
  private static Range<Integer> rangeOf(JCCompilationUnit unit, JCImport statement) {
    return Range.closed(statement.getStartPosition(), statement.getEndPosition(unit.endPositions));
  }
}
