package com.nikodoko.javaimports.environment.maven;

import com.nikodoko.javaimports.environment.shared.SourceFiles;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Finds all .java files in a project. */
// TODO: this will find ALL java files, including in resource folder should it be more
// restrictive?
class MavenProjectFinder implements SourceFiles {
  private final Path root;
  private final List<Path> excluded = new ArrayList<>();

  private MavenProjectFinder(Path root) {
    this.root = root;
  }

  static MavenProjectFinder withRoot(Path root) {
    return new MavenProjectFinder(root);
  }

  MavenProjectFinder exclude(Path... files) {
    this.excluded.addAll(Arrays.asList(files));
    return this;
  }

  @Override
  public List<Path> get() throws IOException {
    return Files.find(root, 100, this::nonExcludedJavaFile).collect(Collectors.toList());
  }

  private boolean nonExcludedJavaFile(Path path, BasicFileAttributes attr) {
    return path.toString().endsWith(".java") && !excluded.contains(path);
  }
}
