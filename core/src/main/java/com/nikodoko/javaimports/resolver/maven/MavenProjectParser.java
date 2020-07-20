package com.nikodoko.javaimports.resolver.maven;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.ImporterException;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;
import com.nikodoko.javaimports.resolver.JavaProject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class MavenProjectParser {
  static final class Result {
    final JavaProject project;
    final Iterable<MavenProjectParserException> errors;

    Result(Iterable<MavenProjectParserException> errors, JavaProject project) {
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

  private final Path root;
  private final List<Path> excluded = new ArrayList<>();

  private final List<MavenProjectParserException> errors = new ArrayList<>();
  private final JavaProject project = new JavaProject();

  private MavenProjectParser(Path root) {
    this.root = root;
  }

  static MavenProjectParser withRoot(Path root) {
    return new MavenProjectParser(root);
  }

  MavenProjectParser excluding(Path... files) {
    this.excluded.addAll(Arrays.asList(files));
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
      return Files.find(root, 100, this::nonExcludedJavaFile).collect(Collectors.toList());
    } catch (IOException e) {
      errors.add(new MavenProjectParserException("could not find files", e));
      return new ArrayList<>();
    }
  }

  private boolean nonExcludedJavaFile(Path path, BasicFileAttributes attr) {
    return path.toString().endsWith(".java") && !excluded.contains(path);
  }

  private void tryToParse(Path path) {
    try {
      parseFile(path);
    } catch (IOException | ImporterException e) {
      errors.add(new MavenProjectParserException("could not parse file at " + path.toString(), e));
    }
  }

  private void parseFile(Path path) throws IOException, ImporterException {
    String source = new String(Files.readAllBytes(path), UTF_8);
    ParsedFile file = new Parser(Options.defaults()).parse(path, source);
    project.add(file);
  }
}
