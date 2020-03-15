package com.nikodoko.javaimports;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.nikodoko.javaimports.fixer.Fixer;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Importer {
  private Importer() {}

  public static String addUsedImports(final Path filename, final String javaCode)
      throws ImporterException {
    ParsedFile f = Parser.parse(javaCode);

    Fixer fixer = Fixer.init(f);
    Fixer.Result r = fixer.tryToFix();

    if (r.done()) {
      return applyFixes(f, javaCode, r);
    }

    Set<ParsedFile> siblings = parseSiblings(filename);
    fixer.addSiblings(siblings);

    r = fixer.tryToFix();

    if (r.done()) {
      return applyFixes(f, javaCode, r);
    }

    // TODO: implement
    return null;
  }

  private static Set<ParsedFile> parseSiblings(final Path filename) throws ImporterException {
    List<String> sources = new ArrayList<>();
    try {
      // Retrieve all java files in the parent directory of filename, excluding filename and not
      // searching recursively
      List<Path> paths =
          Files.find(
                  filename.getParent(),
                  1,
                  (path, attributes) ->
                      path.toString().endsWith(".java")
                          && !path.getFileName().equals(filename.getFileName()))
              .collect(Collectors.toList());

      for (Path p : paths) {
        sources.add(new String(Files.readAllBytes(p), UTF_8));
      }
    } catch (IOException e) {
      throw new IOError(e);
    }

    Set<ParsedFile> siblings = new HashSet<>();
    // XXX: might want to run the parsing on all files, and combine the exceptions instead of
    // stopping at the first incorrect file. That way the user knows all the errors and can fix all
    // of them without rerunning the tool.
    for (String source : sources) {
      siblings.add(Parser.parse(source));
    }

    return siblings;
  }

  private static String applyFixes(ParsedFile file, final String original, Fixer.Result result) {
    // If there are no fixes to do, return the original source code
    if (result.fixes().isEmpty()) {
      return original;
    }

    int insertPos = 0;
    if (file.packageEndPos() > -1) {
      insertPos = original.indexOf(";", file.packageEndPos()) + 1;
    }

    // XXX: we don't really need to order imports alphabetically here, but we do it simply because
    // it's harder to test if the order is not decided.
    String toInsert =
        result.fixes().stream().map(Import::asStatement).sorted().collect(Collectors.joining(""));

    return new StringBuffer(original).insert(insertPos, toInsert).toString();
  }
}
