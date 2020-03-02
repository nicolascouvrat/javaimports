package com.nikodoko.importer;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;

public class Parser {
  // by nikodoko.com
  public static ParsedFile parse(final String javaCode) throws ImporterException {
    JCCompilationUnit unit = getCompilationUnit(javaCode);
    UnresolvedIdentifierScanner scanner = new UnresolvedIdentifierScanner();
    scanner.scan(unit, null);
    return new ParsedFile("", null, scanner.topScope());
  }

  /** Return true if the diagnostic is an error diagnostic. */
  private static boolean isErrorDiagnostic(Diagnostic<?> d) {
    return d.getKind() == Diagnostic.Kind.ERROR;
  }

  @VisibleForTesting
  static JCCompilationUnit getCompilationUnit(final String javaCode) throws ImporterException {
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
    JavacParser parser = parserFactory.newParser(javaCode, false, false, false);
    unit = parser.parseCompilationUnit();
    unit.sourcefile = source;

    List<Diagnostic<? extends JavaFileObject>> errorDiagnostics =
        diagnostics.getDiagnostics().stream()
            .filter(Parser::isErrorDiagnostic)
            .collect(Collectors.toList());

    if (!errorDiagnostics.isEmpty()) {
      throw ImporterException.fromDiagnostics(errorDiagnostics);
    }

    return unit;
  }
}
