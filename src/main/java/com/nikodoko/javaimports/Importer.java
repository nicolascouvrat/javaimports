package com.nikodoko.javaimports;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.nikodoko.javaimports.fixer.Fixer;
import com.nikodoko.javaimports.fixer.FixerOptions;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;
import com.nikodoko.javaimports.parser.ParserOptions;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds all unresolved identifiers in a source file (variables, methods and classes that are used
 * but not declared in this source file) and tries to add appropriate import clauses when needed,
 * using various approaches.
 */
public final class Importer {
  private ImporterOptions options;

  /** An {@code Importer} constructor with default options */
  public Importer() {
    this(ImporterOptions.defaults());
  }

  /**
   * An {@code Importer} constructor.
   *
   * @param options its options.
   */
  public Importer(ImporterOptions options) {
    this.options = options;
  }

  private static ParserOptions parserOptions(ImporterOptions opts) {
    return ParserOptions.builder().debug(opts.debug()).build();
  }

  private static FixerOptions fixerOptions(ImporterOptions opts) {
    return FixerOptions.builder().debug(opts.debug()).build();
  }

  /**
   * Finds all unresolved identifiers in the given {@code javaCode}, and tries to find (and add) as
   * many missing imports as possible using different approaches.
   *
   * <p>The returned string will be a best effort, meaning that all that can be added will be, but
   * that this offers no guarantee that the file will have all of the missing imports added.
   *
   * <p>The approaches are as follows:
   *
   * <ol>
   *   <li>Try to find imports using only what is in the file (if everything is already there, no
   *       need to go further)
   *   <li>Add all package files (assuming they are in the same directory) and use them. FIXME: this
   *       does not handle test files for now (as they are supposedly in a different directory)
   *   <li>TODO: more to come!
   * </ol>
   *
   * @param filename the absolute path to the file to fix
   * @param javaCode the source code to fix
   * @throws ImporterException if the source code cannot be parsed
   */
  public String addUsedImports(final Path filename, final String javaCode)
      throws ImporterException {
    ParsedFile f = new Parser(parserOptions(options)).parse(javaCode);

    Fixer fixer = Fixer.init(f, fixerOptions(options));
    // Initial run with the current file only.
    Fixer.Result r = fixer.tryToFix();

    if (r.done()) {
      // We cannot add any imports at this stage, as we need the package information for that. If we
      // are done, this should mean that the file is complete
      checkArgument(r.fixes().isEmpty(), "expected no fixes but found %s", r.fixes());
      return javaCode;
    }

    // Add package information
    Set<ParsedFile> siblings = parseSiblings(filename);
    fixer.addSiblings(siblings);

    r = fixer.tryToFix();

    if (r.done()) {
      return applyFixes(f, javaCode, r);
    }

    // Do one last ditch effort
    return applyFixes(f, javaCode, fixer.lastTryToFix());
  }

  // Find and parse all java files in the directory of filename, excepting filename itself
  private Set<ParsedFile> parseSiblings(final Path filename) throws ImporterException {
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

  // Add all fixes to the original source code
  private String applyFixes(ParsedFile file, final String original, Fixer.Result result) {
    // If there are no fixes to do, return the original source code
    if (result.fixes().isEmpty()) {
      return original;
    }

    int insertPos = 0;
    if (file.packageEndPos() > -1) {
      insertPos = original.indexOf(";", file.packageEndPos()) + 1;
    }

    // We brutally insert imports just after the package clause, on the same line. This is not
    // pretty, but we do not care: our goal is to find imports, not to organize them nicely. This
    // job is left to other tools.
    // XXX: we don't really need to order imports alphabetically here, but we do it simply because
    // it's harder to test if the order is not decided.
    // XXX: it would however make sense to add a "useless import removal" feature, as javaimports
    // should cover everything that has to do with imports in a file.
    String toInsert =
        result.fixes().stream().map(Import::asStatement).sorted().collect(Collectors.joining(""));

    return new StringBuffer(original).insert(insertPos, toInsert).toString();
  }
}
