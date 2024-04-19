package com.nikodoko.javaimports.parser;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.nikodoko.javaimports.ImporterException;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.parser.internal.JCHelper;
import com.nikodoko.javaimports.parser.internal.UnresolvedIdentifierScanner;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;

/**
 * An "improved" Java parser, that parses the code and analyzes the resulting AST using an {@link
 * UnresolvedIdentifierScanner} to find all declared variables, all unresolved identifiers as well
 * as classes extending another class not declared in the same file.
 */
public class Parser {
  private static Logger log = Logs.getLogger(Parser.class.getName());
  private static final Clock clock = Clock.systemDefaultZone();
  private static final Tag.Key PACKAGE = Tag.withKey("package");
  private static final Tag.Key<Integer> FILE_LENGTH = Tag.withKey("file_length");

  /**
   * Parse the given input (Java code) into a {@link ParsedFile}.
   *
   * @return an optional containing the parsed file, or nothing if the input is empty or contains
   *     only comments
   * @param javaCode the input code
   * @throws ImporterException if the input cannot be parsed
   */
  public Optional<ParsedFile> parse(final Path filename, final String javaCode)
      throws ImporterException {
    var span = Traces.createSpan("Parser.parse", FILE_LENGTH.is(javaCode.length()));
    try (var __ = Traces.activate(span)) {
      return parseInstrumented(filename, javaCode);
    } finally {
      span.finish();
    }
  }

  public Optional<ParsedFile> parseInstrumented(final Path filename, final String javaCode)
      throws ImporterException {
    long start = clock.millis();
    // Parse the code into a compilation unit containing the AST
    JCCompilationUnit unit = getCompilationUnitInstrumented(filename.toString(), javaCode);
    // A lot of what we do relies on having a package clause, consider the file empty if it does not
    // have one.
    if (unit.getPackageName() == null) {
      return Optional.empty();
    }

    // Scan the AST
    UnresolvedIdentifierScanner scanner = new UnresolvedIdentifierScanner();
    try {
      scanner.scan(unit, null);
    } catch (Exception e) {
      throw new RuntimeException("Could not parse file at " + filename, e);
    }

    // Wrap the results in a ParsedFile
    var f = JCHelper.toParsedFileBuilder(unit).topScope(scanner.topScope()).build();
    log.info(String.format("completed parsing in %d ms: %s", clock.millis() - start, f));

    return Optional.of(f);
  }

  /** Return true if the diagnostic is an error diagnostic. */
  private static boolean isErrorDiagnostic(Diagnostic<?> d) {
    return d.getKind() == Diagnostic.Kind.ERROR;
  }

  private JCCompilationUnit getCompilationUnitInstrumented(String filename, String javaCode)
      throws ImporterException {
    var span = Traces.createSpan("Parser.getCompilationUnit", FILE_LENGTH.is(javaCode.length()));
    JCCompilationUnit unit = null;
    try (var __ = Traces.activate(span)) {
      unit = getCompilationUnit(filename, javaCode);
      return unit;
    } finally {
      if (unit != null) {
        Traces.addTags(span, PACKAGE.is(unit.getPackageName()));
      }

      span.finish();
    }
  }

  // This should not be public, but is used in test
  public static JCCompilationUnit getCompilationUnit(final String filename, final String javaCode)
      throws ImporterException {
    Context ctx = new Context();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    ctx.put(DiagnosticListener.class, diagnostics);
    JCCompilationUnit unit;
    JavacFileManager fileManager = new JavacFileManager(ctx, true, UTF_8);

    try {
      fileManager.setLocation(StandardLocation.PLATFORM_CLASS_PATH, ImmutableList.of());
    } catch (IOException e) {
      // impossible
      throw new IOError(e);
    }

    // This is used by the parser to report syntax errors (the parser will refer to this file)
    SimpleJavaFileObject source =
        new SimpleJavaFileObject(URI.create("source"), JavaFileObject.Kind.SOURCE) {
          @Override
          public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return javaCode;
          }
        };
    Log.instance(ctx).useSource(source);

    ParserFactory parserFactory = ParserFactory.instance(ctx);
    // It is necessary to set keepEndPos to true in order to retrieve the end position of
    // expressions like the package clause, etc.
    JavacParser parser = parserFactory.newParser(javaCode, false, /* keepEndPos= */ true, false);
    unit = parser.parseCompilationUnit();
    unit.sourcefile = source;

    List<Diagnostic<? extends JavaFileObject>> errorDiagnostics =
        diagnostics.getDiagnostics().stream()
            .filter(Parser::isErrorDiagnostic)
            .collect(Collectors.toList());

    if (!errorDiagnostics.isEmpty()) {
      throw ImporterException.fromDiagnostics(filename, errorDiagnostics);
    }

    return unit;
  }
}
