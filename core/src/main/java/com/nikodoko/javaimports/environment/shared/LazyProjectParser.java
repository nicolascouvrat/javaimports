package com.nikodoko.javaimports.environment.shared;

import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.environment.EnvironmentException;
import com.nikodoko.javaimports.parser.ParsedFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class LazyProjectParser {
  public record Result(LazyJavaProject project, Iterable<EnvironmentException> errors) {}

  private record ParseResult(Optional<ParsedFile> parsed, EnvironmentException error) {}

  private final SourceFiles srcs;
  private final Selector refPkg;

  public LazyProjectParser(Selector refPkg, SourceFiles srcs) {
    this.refPkg = refPkg;
    this.srcs = srcs;
  }

  public Result parse() {
    List<Path> files;
    try {
      files = srcs.get();
    } catch (IOException e) {
      return new Result(
          new LazyJavaProject(List.of()),
          List.of(new EnvironmentException("could not find files", e)));
    }

    var project =
        new LazyJavaProject(files.stream().map(f -> new LazyParsedFile(refPkg, f)).toList());
    return new Result(project, List.of());
  }
}
