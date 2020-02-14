package com.nikodoko.importer;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;

// TODO: are those the correct imports? Why does it not work with org.openjdk?

public final class ImportFixer {
  public ImportFixer() {}

  /** Return true if the diagnostic is an error diagnostic. */
  public static boolean isErrorDiagnostic(Diagnostic<?> d) {
    return d.getKind() == Diagnostic.Kind.ERROR;
  }

  public static void addUsedImports(final String javaCode) throws ImporterException {
    Context ctx = new Context();
    JCCompilationUnit unit = parse(ctx, javaCode);
  }

  private static JCCompilationUnit parse(Context ctx, final String javaCode)
      throws ImporterException {
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    ctx.put(DiagnosticListener.class, diagnostics);
    JCCompilationUnit unit;
    JavacFileManager fileManager = new JavacFileManager(ctx, true, UTF_8);
    // TODO: simplify with ImmutableList.of()
    List<File> path = new ArrayList<>();
    try {
      fileManager.setLocation(StandardLocation.PLATFORM_CLASS_PATH, path);
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
    // TODO: why would i use iterables here instead of a stream?
    List<Diagnostic<? extends JavaFileObject>> errorDiagnostics =
        diagnostics.getDiagnostics().stream()
            .filter(ImportFixer::isErrorDiagnostic)
            .collect(Collectors.toList());

    if (!errorDiagnostics.isEmpty()) {
      throw ImporterException.fromDiagnostics(errorDiagnostics);
    }

    return unit;
  }
}
