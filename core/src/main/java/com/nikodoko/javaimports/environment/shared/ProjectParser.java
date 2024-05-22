package com.nikodoko.javaimports.environment.shared;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.nikodoko.javaimports.ImporterException;
import com.nikodoko.javaimports.common.Utils;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.environment.EnvironmentException;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;
import io.opentracing.Span;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ProjectParser {
  public record Result(JavaProject project, Iterable<EnvironmentException> errors) {}

  private record ParseResult(Optional<ParsedFile> parsed, EnvironmentException error) {}

  private final SourceFiles srcs;
  private final Executor executor;

  public ProjectParser(SourceFiles srcs, Executor executor) {
    this.srcs = srcs;
    this.executor = executor;
  }

  public Result parseAll() {
    var span = Traces.createSpan("ProjectParser.parseAll");
    try (var __ = Traces.activate(span)) {
      return parseAllInstrumented(span);
    } finally {
      span.finish();
    }
  }

  private Result parseAllInstrumented(Span span) {
    List<Path> files;
    try {
      files = srcs.get();
    } catch (IOException e) {
      return new Result(
          new JavaProject(), List.of(new EnvironmentException("could not find files", e)));
    }

    var tasks = files.stream().map(f -> tryParseAsync(span, f)).toList();

    var project = new JavaProject();
    var errors = new ArrayList<EnvironmentException>();
    Utils.sequence(tasks)
        .join()
        .forEach(
            r -> {
              if (r.error() != null) {
                errors.add(r.error());
              }

              if (r.parsed() != null && r.parsed().isPresent()) {
                project.add(r.parsed().get());
              }
            });

    return new Result(project, errors);
  }

  private CompletableFuture<ParseResult> tryParseAsync(Span span, Path path) {
    return CompletableFuture.supplyAsync(() -> tryParse(span, path), executor);
  }

  private ParseResult tryParse(Span span, Path path) {
    try (var __ = Traces.activate(span)) {
      return new ParseResult(parse(path), null);
    } catch (IOException | ImporterException e) {
      return new ParseResult(null, new EnvironmentException("could not parse file at " + path, e));
    }
  }

  private Optional<ParsedFile> parse(Path path) throws IOException, ImporterException {
    var src = new String(Files.readAllBytes(path), UTF_8);
    return new Parser().parse(path, src);
  }
}
