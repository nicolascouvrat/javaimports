package com.nikodoko.javaimports.resolver;

import com.nikodoko.javaimports.parser.Import;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class MavenDependencyResolver {
  private static final String SUBCLASS_SEPARATOR = "$";
  // Supports "exotic" versioning, like guava's "26.0-jre"
  private static final Pattern VERSION_REGEX =
      Pattern.compile("(?:\\S+)?(?<version>\\d+(?:\\.\\d+)+)(?:\\S+)?");
  final Path fileBeingResolved;
  final Path repository;

  MavenDependencyResolver(Path fileBeingResolved, Path repository) {
    this.fileBeingResolved = fileBeingResolved;
    this.repository = repository;
  }

  List<ImportWithDistance> resolve(List<MavenDependency> dependencies) throws IOException {
    List<ImportWithDistance> imports = new ArrayList<>();
    for (MavenDependency dependency : dependencies) {
      imports.addAll(resolveDependency(dependency));
    }

    return imports;
  }

  private List<ImportWithDistance> resolveDependency(MavenDependency dependency)
      throws IOException {
    return scanJar(jarPath(dependency));
  }

  private List<ImportWithDistance> scanJar(Path jar) throws IOException {
    List<ImportWithDistance> imports = new ArrayList<>();
    try (JarInputStream in = new JarInputStream(new FileInputStream(jar.toString()))) {
      JarEntry entry;
      while ((entry = in.getNextJarEntry()) != null) {
        // XXX: this will get all classes, including private and protected ones
        if (entry.getName().endsWith(".class")) {
          Import i = parseImport(Paths.get(entry.getName()));
          imports.add(ImportWithDistance.of(i, jar, fileBeingResolved));
        }
      }
    }

    return imports;
  }

  private Import parseImport(Path jarEntry) {
    String pkg = jarEntry.getParent().toString().replace("/", ".");
    String filename = jarEntry.getFileName().toString();
    String name = filename.substring(0, filename.lastIndexOf("."));
    if (!name.contains(SUBCLASS_SEPARATOR)) {
      return new Import(pkg, name, false);
    }

    // Make the subclass addressable by its name
    String extraPkg =
        name.substring(0, name.lastIndexOf(SUBCLASS_SEPARATOR)).replace(SUBCLASS_SEPARATOR, ".");
    String subclassName = name.substring(name.lastIndexOf(SUBCLASS_SEPARATOR) + 1, name.length());
    return new Import(String.join(".", pkg, extraPkg), subclassName, false);
  }

  private Path jarPath(MavenDependency dependency) throws IOException {
    Path dependencyRepository =
        Paths.get(
            repository.toString(), dependency.groupId.replace(".", "/"), dependency.artifactId);
    String version = dependency.version;
    if (!dependency.hasPlainVersion()) {
      version = getLatestAvailableVersion(dependencyRepository).name;
    }

    return Paths.get(
        dependencyRepository.toString(), version, jarName(dependency.artifactId, version));
  }

  private String jarName(String artifactId, String version) {
    return String.format("%s-%s.jar", artifactId, version);
  }

  private MavenDependencyVersion getLatestAvailableVersion(Path dependencyRepository)
      throws IOException {
    List<MavenDependencyVersion> versions =
        Files.find(dependencyRepository, 1, (path, attributes) -> Files.isDirectory(path))
            .map(p -> MavenDependencyVersion.of(p.getFileName().toString()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    return Collections.max(versions);
  }
}
