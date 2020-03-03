package com.nikodoko.javaimports;

import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;

public final class Importer {
  private Importer() {}

  public static void addUsedImports(final String javaCode) throws ImporterException {
    ParsedFile f = Parser.parse(javaCode);
    System.out.println(f);
  }
}
