package com.nikodoko.javaimports.resolver.maven;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.nikodoko.javaimports.ImporterException;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class MavenProjectScanner {
  static final class Result {
    final Iterable<ParsedFile> files;
    final Iterable<ProjectScannerException> errors;

    Result(Iterable<ParsedFile> files, Iterable<ProjectScannerException> errors) {
      this.files = files;
      this.errors = errors;
    }

    public String toString() {
      return MoreObjects.toStringHelper(this).add("files", files).add("errors", errors).toString();
    }
  }

  private final Path root;
  private final List<Path> excluded = new ArrayList<>();

  private final Map<String, Set<ParsedFile>> files = new HashMap<>();
  private final List<ProjectScannerException> errors = new ArrayList<>();
  private boolean scanCompleted;

  private MavenProjectScanner(Path root) {
    this.root = root;
  }

  static MavenProjectScanner withRoot(Path root) {
    return new MavenProjectScanner(root);
  }

  MavenProjectScanner excluding(Path... files) {
    this.excluded.addAll(Arrays.asList(files));
    return this;
  }

  Result scanFilesInPackage(String pkg) {
    if (!scanCompleted) {
      scan();
    }

    return new Result(files.getOrDefault(pkg, new HashSet<>()), errors);
  }

  Result scanAllFiles() throws ProjectScannerException {
    if (!scanCompleted) {
      scan();
    }

    return new Result(Iterables.concat(files.values()), errors);
  }

  private void scan() {
    for (Path javaFile : tryToFindAllFiles()) {
      tryToParse(javaFile);
    }

    scanCompleted = true;
  }

  private List<Path> tryToFindAllFiles() {
    try {
      return Files.find(root, 100, this::nonExcludedJavaFile).collect(Collectors.toList());
    } catch (IOException e) {
      errors.add(new ProjectScannerException("could not find files", e));
      return new ArrayList<>();
    }
  }

  private boolean nonExcludedJavaFile(Path path, BasicFileAttributes attr) {
    return path.toString().endsWith(".java") && !excluded.contains(path);
  }

  private void tryToParse(Path path) {
    try {
      ParsedFile file = parseFile(path);
      Set<ParsedFile> filesInPackage = files.getOrDefault(file.packageName(), new HashSet<>());
      filesInPackage.add(file);
      files.put(file.packageName(), filesInPackage);
    } catch (IOException | ImporterException e) {
      errors.add(new ProjectScannerException("could not parse file at " + path.toString(), e));
    }
  }

  private ParsedFile parseFile(Path path) throws IOException, ImporterException {
    String source = new String(Files.readAllBytes(path), UTF_8);
    return new Parser(Options.defaults()).parse(path, source);
  }
}
