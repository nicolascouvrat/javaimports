package com.nikodoko.javaimports.environment.shared;

import com.nikodoko.javaimports.common.JavaSourceFile;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Utils;
import com.nikodoko.javaimports.common.telemetry.Traces;
import io.opentracing.Span;
import java.util.List;
import java.util.concurrent.Executor;

public class LazyJavaProject {
  private final List<LazyParsedFile> files;

  LazyJavaProject(List<LazyParsedFile> files) {
    this.files = files;
  }

  public List<JavaSourceFile> filesInPackage(Selector pkg) {
    return files.stream().filter(f -> f.pkg().equals(pkg)).map(JavaSourceFile.class::cast).toList();
  }

  public List<? extends JavaSourceFile> allFiles() {
    return files;
  }

  public void eagerlyParse(Executor e) {
    var span = Traces.createSpan("LazyJavaProject.eagerlyParse");
    try (var __ = Traces.activate(span)) {
      eagerlyParseInstrumented(span, e);
    } finally {
      span.finish();
    }
  }

  private void eagerlyParseInstrumented(Span span, Executor e) {
    var tasks = files.stream().map(f -> f.parseAsync(e)).toList();
    Utils.sequence(tasks).join();
  }
}
