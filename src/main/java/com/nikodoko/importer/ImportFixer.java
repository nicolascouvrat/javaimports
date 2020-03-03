package com.nikodoko.importer;

import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;

// TODO: are those the correct imports? Why does it not work with org.openjdk?

public final class ImportFixer {
  public ImportFixer() {}

  private static String getPackageRootName(JCExpression expr) {
    JCExpression selected = expr;
    while (!(selected instanceof JCIdent)) {
      selected = ((JCFieldAccess) selected).getExpression();
    }
    return ((JCIdent) selected).getName().toString();
  }

  public static void addUsedImports(final String javaCode) throws ImporterException {
    ParsedFile f = Parser.parse(javaCode);
    System.out.println(f);
  }
}
