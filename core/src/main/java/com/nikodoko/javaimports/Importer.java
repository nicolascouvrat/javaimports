package com.nikodoko.javaimports;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.Range;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.common.telemetry.Metrics;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.environment.Environments;
import com.nikodoko.javaimports.fixer.Fixer;
import com.nikodoko.javaimports.fixer.Result;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Finds all unresolved identifiers in a source file (variables, methods and classes that are used
 * but not declared in this source file) and tries to add appropriate import clauses when needed,
 * using various approaches.
 */
public final class Importer {
  private static final Logger log = Logs.getLogger(Importer.class.getName());
  private static final Clock clock = Clock.systemDefaultZone();

  private Options options;
  private Parser parser;

  /** An {@code Importer} constructor with default options */
  public Importer() {
    this(Options.defaults());
  }

  /**
   * An {@code Importer} constructor.
   *
   * @param options its options.
   */
  public Importer(Options options) {
    this.options = options;
    this.parser = new Parser();
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
    Metrics.count("importer.runs", 1);
    long start = clock.millis();
    var span = Traces.createSpan("Importer.addUsedImports");
    try (var __ = Traces.activate(span)) {
      Optional<ParsedFile> f = parser.parse(filename, javaCode, null);
      if (f.isEmpty()) {
        log.log(Level.WARNING, "file is empty");
        return javaCode;
      }

      Result fixes = getFixes(filename, f.get());
      return applyFixes(f.get(), javaCode, fixes);
    } finally {
      span.finish();
      var elapsed = clock.millis() - start;
      Metrics.gauge("importer.duration", elapsed);
      log.log(Level.INFO, String.format("total time: %d ms", elapsed));
    }
  }

  private Result getFixes(Path filename, ParsedFile f) throws ImporterException {
    var span = Traces.createSpan("Importer.getFixes");
    try (var __ = Traces.activate(span)) {
      return getFixesInstrumented(filename, f);
    } finally {
      span.finish();
    }
  }

  private Result getFixesInstrumented(Path filename, ParsedFile f) throws ImporterException {
    Fixer fixer = Fixer.init(f);
    // Initial run with the current file only.
    Result r = fixer.tryToFix();
    if (r.done()) {
      // We cannot add any imports at this stage, as we need the package information for that. If we
      // are done, this should mean that the file is complete
      log.log(Level.INFO, "file is complete");
      checkArgument(r.fixes().isEmpty(), "expected no fixes but found %s", r.fixes());
      return r;
    }

    // Add package information
    Set<ParsedFile> siblings = parseSiblings(filename, f.pkg());
    fixer.addSiblings(siblings);
    r = fixer.tryToFix();

    if (r.done()) {
      return r;
    }

    // Files in a same package can be in a different folder (if we are resolving a test file for
    // example) and the siblings we've added so far are only the ones found in the same folder.
    // If other files in the package contain identifiers that also are in the standard library, we
    // want to resolve them before so as to avoid adding uneeded imports, so we need to add both the
    // stdlib provider and the resolver at the same time.
    fixer.addStdlibProvider(options.stdlib());
    fixer.addEnvironment(Environments.autoSelect(filename, f.pkg(), options));

    return fixer.lastTryToFix();
  }

  // Find and parse all java files in the directory of filename, excepting filename itself
  private Set<ParsedFile> parseSiblings(final Path filename, Selector pkg)
      throws ImporterException {
    var span = Traces.createSpan("Importer.parseSiblings");
    try (var __ = Traces.activate(span)) {
      return parseSiblingsInstrumented(filename, pkg);
    } finally {
      span.finish();
    }
  }

  private Set<ParsedFile> parseSiblingsInstrumented(final Path filename, Selector pkg)
      throws ImporterException {
    Map<Path, String> sources = new HashMap<>();
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
        sources.put(p, new String(Files.readAllBytes(p), UTF_8));
      }
    } catch (IOException e) {
      throw new IOError(e);
    }

    Set<ParsedFile> siblings = new HashSet<>();
    List<ImporterException> exceptions = new ArrayList<>();
    // Try to parse all files even if one is invalid (so that the user can fix everything without
    // rerunning the tool), but fail if one is wrong.
    for (Map.Entry<Path, String> source : sources.entrySet()) {
      try {
        parser.parse(source.getKey(), source.getValue(), pkg).ifPresent(siblings::add);
      } catch (ImporterException e) {
        exceptions.add(e);
      }
    }

    if (!exceptions.isEmpty()) {
      throw ImporterException.combine(exceptions);
    }

    return siblings;
  }

  private String buildImportStatements(Set<Import> fixes) {
    // XXX: we don't really need to order imports alphabetically here, but we do it simply because
    // it's harder to test if the order is not deterministic
    return fixes.stream().map(Import::toStatement).sorted().collect(Collectors.joining(""));
  }

  // Add all fixes to the original source code
  private String applyFixes(ParsedFile file, final String original, Result result) {
    if (result.fixes().isEmpty() && file.duplicates().isEmpty()) {
      return original;
    }

    String statements = buildImportStatements(result.fixes());
    int insertPos = 0;
    if (file.packageEndPos() > -1) {
      insertPos = original.indexOf(";", file.packageEndPos()) + 1;
    }

    // We brutally insert imports just after the package clause, on the same line. This is not
    // pretty, but we do not care: our goal is to find imports, not to organize them nicely. This
    // job is left to other tools.
    // XXX: it would however make sense to add a "useless import removal" feature, as javaimports
    // should cover everything that has to do with imports in a file.
    StringBuilder sb = new StringBuilder(original);
    sb.insert(insertPos, statements);

    for (Range<Integer> duplicate : file.duplicates()) {
      sb.delete(duplicate.lowerEndpoint(), duplicate.upperEndpoint());
    }
    return sb.toString();
  }
}
