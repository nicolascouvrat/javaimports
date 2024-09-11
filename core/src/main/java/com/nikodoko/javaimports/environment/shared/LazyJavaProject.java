package com.nikodoko.javaimports.environment.shared;

import com.nikodoko.javaimports.common.JavaSourceFile;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Utils;
import com.nikodoko.javaimports.common.telemetry.Traces;
import io.opentracing.Span;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class LazyJavaProject {
  private final Map<Dependency.Kind, List<LazyParsedFile>> allFiles;
  private final Set<LazyParsedFile> available = new HashSet<>();

  // TODO: remove me
  LazyJavaProject(List<LazyParsedFile> files) {
    this.allFiles = Map.of(Dependency.Kind.DIRECT, files);
    makeAvailable(Dependency.Kind.DIRECT);
  }

  public LazyJavaProject(Selector refPkg, List<? extends Dependency> srcs) {
    this.allFiles =
        srcs.stream()
            .collect(
                Collectors.groupingBy(
                    Dependency::kind,
                    Collectors.mapping(
                        src -> new LazyParsedFile(refPkg, src.path()), Collectors.toList())));
    makeAvailable(Dependency.Kind.DIRECT);
  }

  public void includeTransitive() {
    makeAvailable(Dependency.Kind.TRANSITIVE);
  }

  private void makeAvailable(Dependency.Kind kind) {
    available.addAll(allFiles.getOrDefault(kind, List.of()));
  }

  public List<JavaSourceFile> filesInPackage(Selector pkg) {
    return available.stream()
        .filter(f -> f.pkg().equals(pkg))
        .map(JavaSourceFile.class::cast)
        .toList();
  }

  public List<? extends JavaSourceFile> allFiles() {
    return available.stream().toList();
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
    var tasks = available.stream().map(f -> f.parseAsync(e)).toList();
    Utils.sequence(tasks).join();
  }
}
