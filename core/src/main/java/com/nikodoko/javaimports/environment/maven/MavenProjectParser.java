package com.nikodoko.javaimports.environment.maven;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.ImporterException;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.environment.JavaProject;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/** Parses all java files in a given project. */
class MavenProjectParser {
  static final class Result {
    final JavaProject project;
    final Iterable<MavenEnvironmentException> errors;

    Result(Iterable<MavenEnvironmentException> errors, JavaProject project) {
      this.errors = errors;
      this.project = project;
    }

    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("project", project)
          .add("errors", errors)
          .toString();
    }
  }

  // TODO: maybe abstract this to a utils class
  static final class Pair<L, R> {
    final L left;
    final R right;

    Pair(L left, R right) {
      this.left = left;
      this.right = right;
    }
  }

  private final MavenProjectFinder finder;
  private final Options options;

  private final List<MavenEnvironmentException> errors = new ArrayList<>();
  private final JavaProject project = new JavaProject();

  public MavenProjectParser(Path root, Options options) {
    this.finder = MavenProjectFinder.withRoot(root);
    this.options = options;
  }

  MavenProjectParser excluding(Path... files) {
    finder.exclude(files);
    return this;
  }

  Result parseAll() {
    var futures =
        tryToFindAllFiles().stream()
            .map(path -> CompletableFuture.supplyAsync(() -> tryToParse(path), options.executor()))
            .collect(Collectors.toList());

    CompletableFuture.allOf(futures.stream().toArray(CompletableFuture[]::new)).join();
    futures.stream()
        .map(CompletableFuture::join)
        .forEach(
            pair -> {
              if (pair.left != null && pair.left.isPresent()) {
                project.add(pair.left.get());
              }

              if (pair.right != null) {
                errors.add(pair.right);
              }
            });

    return new Result(errors, project);
  }

  private List<Path> tryToFindAllFiles() {
    try {
      return finder.findAll();
    } catch (IOException e) {
      errors.add(new MavenEnvironmentException("could not find files", e));
      return List.of();
    }
  }

  private Pair<Optional<ParsedFile>, MavenEnvironmentException> tryToParse(Path path) {
    try {
      var result = new Pair(parseFile(path), null);
      return result;
    } catch (IOException | ImporterException e) {
      return new Pair(
          null, new MavenEnvironmentException("could not parse file at " + path.toString(), e));
    }
  }

  private Optional<ParsedFile> parseFile(Path path) throws IOException, ImporterException {
    String source = new String(Files.readAllBytes(path), UTF_8);
    return new Parser(options).parse(path, source);
  }
}
