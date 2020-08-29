package com.nikodoko.javaimports.environment.maven;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.ImporterException;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;
import com.nikodoko.javaimports.environment.JavaProject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

  private final MavenProjectFinder finder;

  private final List<MavenEnvironmentException> errors = new ArrayList<>();
  private final JavaProject project = new JavaProject();

  private MavenProjectParser(Path root) {
    this.finder = MavenProjectFinder.withRoot(root);
  }

  static MavenProjectParser withRoot(Path root) {
    return new MavenProjectParser(root);
  }

  MavenProjectParser excluding(Path... files) {
    finder.exclude(files);
    return this;
  }

  Result parseAll() {
    for (Path javaFile : tryToFindAllFiles()) {
      tryToParse(javaFile);
    }

    return new Result(errors, project);
  }

  private List<Path> tryToFindAllFiles() {
    try {
      return finder.findAll();
    } catch (IOException e) {
      errors.add(new MavenEnvironmentException("could not find files", e));
      return new ArrayList<>();
    }
  }

  private void tryToParse(Path path) {
    try {
      parseFile(path);
    } catch (IOException | ImporterException e) {
      errors.add(new MavenEnvironmentException("could not parse file at " + path.toString(), e));
    }
  }

  private void parseFile(Path path) throws IOException, ImporterException {
    String source = new String(Files.readAllBytes(path), UTF_8);
    ParsedFile file = new Parser(Options.defaults()).parse(path, source);
    project.add(file);
  }
}
