package com.nikodoko.javaimports.environment.maven;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Resolves Maven dependencies to their location on disk. */
class MavenDependencyResolver {
  static class PrimaryArtifact {
    final Path pom;
    final Path jar;

    PrimaryArtifact(Path pom, Path jar) {
      this.pom = pom;
      this.jar = jar;
    }

    public String toString() {
      return MoreObjects.toStringHelper(this).add("pom", pom).add("jar", jar).toString();
    }
  }

  private final Path repository;

  private MavenDependencyResolver(Path repository) {
    this.repository = repository;
  }

  static MavenDependencyResolver withRepository(Path repository) {
    return new MavenDependencyResolver(repository);
  }

  PrimaryArtifact resolve(MavenDependency dependency) throws IOException {
    var coordinates = dependency.coordinates();
    return resolve(coordinates);
  }

  PrimaryArtifact resolve(MavenCoordinates coordinates) throws IOException {
    var span =
        Traces.createSpan("MavenDependencyResolver.resolve", new Tag("coordinates", coordinates));
    try (var __ = Traces.activate(span)) {
      return resolveInstrumented(coordinates);
    } finally {
      span.finish();
    }
  }

  private PrimaryArtifact resolveInstrumented(MavenCoordinates coordinates) throws IOException {
    var artifactPath = artifactPath(coordinates);
    var jarSuffix = jarSuffix(coordinates);
    return new PrimaryArtifact(
        artifactPath.resolveSibling(artifactPath.getFileName() + ".pom"),
        artifactPath.resolveSibling(artifactPath.getFileName() + jarSuffix));
  }

  private String jarSuffix(MavenCoordinates coordinates) {
    var classifierSuffix = coordinates.maybeClassifier().map(c -> "-" + c).orElse("");
    var typeSuffix = coordinates.type().equals("test-jar") ? "-tests.jar" : ".jar";
    return classifierSuffix + typeSuffix;
  }

  private Path artifactPath(MavenCoordinates coordinates) throws IOException {
    Path dependencyRepository = directoryFor(coordinates);
    var maybeVersion = coordinates.maybeVersion();
    var versionString = maybeVersion.map(MavenString::toString).orElse("");
    if (maybeVersion.isEmpty() || maybeVersion.get().hasPropertyReferences()) {
      // If we get there, that means that we did not find enough information in the POM. We don't
      // know what version is being used, so we just use the first one we find.
      versionString = getFirstAvailableVersion(dependencyRepository);
    }

    return Paths.get(
        dependencyRepository.toString(),
        versionString,
        artifactName(coordinates.artifactId(), versionString));
  }

  private Path directoryFor(MavenCoordinates coordinates) {
    return Paths.get(
        repository.toString(), coordinates.groupId().replace(".", "/"), coordinates.artifactId());
  }

  private String artifactName(String artifactId, String version) {
    return String.format("%s-%s", artifactId, version);
  }

  private String getFirstAvailableVersion(Path dependencyRepository) throws IOException {
    var maybeVersion =
        Files.find(
                dependencyRepository,
                1,
                (path, attributes) -> Files.isDirectory(path) && !path.equals(dependencyRepository))
            .map(p -> p.getFileName().toString())
            // We sort it to have a deterministic result in tests
            .sorted()
            .findFirst();
    if (maybeVersion.isEmpty()) {
      throw new RuntimeException("Did not find any available version in " + dependencyRepository);
    }

    return maybeVersion.get();
  }
}
