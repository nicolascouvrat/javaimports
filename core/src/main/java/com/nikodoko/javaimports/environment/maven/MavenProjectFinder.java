package com.nikodoko.javaimports.environment.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// TODO: this will find ALL java files, including in resource folder
// should it be more restrictive?
class MavenProjectFinder {
  private final Path root;
  private final List<Path> excluded = new ArrayList<>();

  private MavenProjectFinder(Path root) {
    this.root = root;
  }

  static MavenProjectFinder withRoot(Path root) {
    return new MavenProjectFinder(root);
  }

  void exclude(Path... files) {
    this.excluded.addAll(Arrays.asList(files));
  }

  List<Path> findAll() throws IOException {
    return Files.find(root, 100, this::nonExcludedJavaFile).collect(Collectors.toList());
  }

  private boolean nonExcludedJavaFile(Path path, BasicFileAttributes attr) {
    return path.toString().endsWith(".java") && !excluded.contains(path);
  }
}
