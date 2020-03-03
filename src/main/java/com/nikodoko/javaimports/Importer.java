package com.nikodoko.javaimports;

import com.nikodoko.javaimports.fixer.Fixer;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;
import java.nio.file.Path;
import java.util.Set;

public final class Importer {
  private Importer() {}

  public static void addUsedImports(final Path filename, final String javaCode)
      throws ImporterException {
    ParsedFile f = Parser.parse(javaCode);

    Fixer fixer = Fixer.init(f);
    Fixer.Result r = fixer.tryToFix();

    System.out.println(r);

    if (r.done()) {
      // TODO: implement
      return;
    }

    Set<ParsedFile> siblings = parseSiblings(filename);
    fixer.addSiblings(siblings);

    r = fixer.tryToFix();

    if (r.done()) {
      // TODO: implement
      return;
    }

    // TODO: implement
  }

  private static Set<ParsedFile> parseSiblings(final Path filename) {
    // TODO: implement
    return null;
  }
}
