package com.nikodoko.javaimports.environment.maven;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.common.telemetry.Traces;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Finds all dependencies in a Maven project by parsing POM files. */
class MavenDependencyFinder {
  static final class Result {
    final List<MavenDependency> dependencies = new ArrayList<>();
    final List<MavenEnvironmentException> errors = new ArrayList<>();

    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("dependencies", dependencies)
          .add("errors", errors)
          .toString();
    }
  }

  private static final Path POM = Paths.get("pom.xml");
  private final MavenRepository repository;

  MavenDependencyFinder(MavenRepository repository) {
    this.repository = repository;
  }

  Result findAll(Path moduleRoot) {
    var span = Traces.createSpan("MavenDependencyFinder.findAll");
    try (var __ = Traces.activate(span)) {
      return findAllInstrumented(moduleRoot);
    } finally {
      span.finish();
    }
  }

  private Result findAllInstrumented(Path moduleRoot) {
    var loaded = MavenPomLoader.load(moduleRoot.resolve(POM));
    var result = new Result();

    var pom = loaded.pom;
    var errors = new ArrayList<>(loaded.errors);
    var currentPomDir = moduleRoot;
    while (hasRelativeParentPath(pom)) {
      // We need to normalize because the relative parent path often includes the special name ..
      var parentPath = currentPomDir.resolve(relativeParentPomPath(pom)).normalize();
      loaded = MavenPomLoader.load(parentPath);
      errors.addAll(loaded.errors);
      pom.merge(loaded.pom);
      currentPomDir = parentPath.getParent();
    }

    // According to the maven documentation, managed dependencies with scope "import" should be
    // replaced with the effective list of dependencies in the specified POM's
    // <dependencyManagement> section. See:
    // https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Scope
    var managedDepsToAdd =
        pom.managedDependencies().stream()
            .filter(d -> d.hasScope("import"))
            .flatMap(d -> repository.getManagedDependencies(d).stream())
            .collect(Collectors.toList());
    pom.merge(FlatPom.builder().managedDependencies(managedDepsToAdd).build());

    result.dependencies.addAll(pom.dependencies());
    result.errors.addAll(errors);

    return result;
  }

  private boolean hasRelativeParentPath(FlatPom pom) {
    return pom.maybeParent().flatMap(p -> p.maybeRelativePath).isPresent();
  }

  private Path relativeParentPomPath(FlatPom pom) {
    var parent = pom.maybeParent().flatMap(p -> p.maybeRelativePath).get();
    if (parent.endsWith(POM)) {
      return parent;
    }

    // Consider that we had a directory, attempt to find a pom in it
    return parent.resolve(POM);
  }
}
