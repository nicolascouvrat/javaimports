package com.nikodoko.javaimports.environment.maven;

import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import io.opentracing.Span;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LocalMavenRepository implements MavenRepository {
  private static Logger log = Logger.getLogger(LocalMavenRepository.class.getName());
  private static final Path DEFAULT_REPOSITORY =
      Paths.get(System.getProperty("user.home"), ".m2/repository");

  private final MavenDependencyResolver resolver;
  private final Options options;

  LocalMavenRepository(MavenDependencyResolver resolver, Options options) {
    this.resolver = resolver;
    this.options = options;
  }

  @Override
  public List<MavenDependency> getManagedDependencies(MavenDependency dependency) {
    var pom = effectivePom(dependency);
    return new ArrayList<>(pom.managedDependencies());
  }

  // TODO: tentative API, this is most likely only for tests
  public List<MavenDependency> getDirectDependencies(MavenDependency dependency) {
    return effectivePom(dependency).dependencies();
  }

  public Collection<MavenDependency> getTransitiveDependencies(
      List<MavenDependency> directDependencies, int maxDepth) {
    var span = Traces.createSpan("LocalMavenRepository.getAllTransitiveDependencies");
    AtomicInteger conflicts = new AtomicInteger(0);
    try (var __ = Traces.activate(span)) {
      var directCoordinates =
          directDependencies.stream().map(d -> d.coordinates()).collect(Collectors.toList());
      Set<MavenCoordinates> visited = ConcurrentHashMap.newKeySet();
      visited.addAll(directCoordinates);
      var versionlessDirectDependencies =
          directCoordinates.stream().map(MavenCoordinates::hideVersion).collect(Collectors.toSet());
      var byVersionlessCoordinates =
          directDependencies.parallelStream()
              .flatMap(
                  d ->
                      getTransitiveDependencies(
                          span, d, visited, versionlessDirectDependencies, maxDepth)
                          .stream())
              .collect(
                  Collectors.toMap(
                      d -> d.dependency.coordinates().hideVersion(),
                      d -> d,
                      (d1, d2) -> {
                        conflicts.incrementAndGet();
                        if (d1.depth < d2.depth) {
                          return d1;
                        }

                        if (d2.depth < d1.depth) {
                          return d2;
                        }

                        return d1;
                      }));

      return byVersionlessCoordinates.values().stream()
          .map(d -> d.dependency)
          .collect(Collectors.toList());
    } finally {
      Traces.addTags(span, new Tag("conflicts", conflicts.get()));
      span.finish();
    }
  }

  // When encounting the same dependency with two different versions, Maven wil pick the nearest
  // one. This allow us to track how far down the dependency tree a transitive dependency was found.
  private static class DependencyWithDepth {
    final MavenDependency dependency;
    final int depth;

    DependencyWithDepth(MavenDependency dependency, int depth) {
      this.dependency = dependency;
      this.depth = depth;
    }

    @Override
    public String toString() {
      return dependency.toString();
    }
  }

  // WARNING: dependency is considered to be a direct dependency of something already, so it will
  // apply filtering rules
  private List<DependencyWithDepth> getTransitiveDependencies(
      Span parentSpan,
      MavenDependency target,
      Set<MavenCoordinates> visited,
      Set<MavenCoordinates.Versionless> versionlessDirectDependencies,
      int maxDepth) {
    try (var __ = Traces.activate(parentSpan)) {
      var span =
          Traces.createSpan(
              "LocalMavenRepository.getTransitiveDependencies", new Tag("target", target));
      try (var ___ = Traces.activate(span)) {
        Set<MavenDependency.Exclusion> exclusions = ConcurrentHashMap.newKeySet();
        List<DependencyWithDepth> found = new ArrayList<>();
        List<MavenDependency> nextLayer = new ArrayList<>();
        exclusions.addAll(target.exclusions());
        nextLayer.add(target);

        var depth = 0;
        while (!nextLayer.isEmpty()) {
          if (maxDepth >= 0 && depth >= maxDepth) {
            break;
          }

          depth += 1;
          var transitiveDeps =
              nextLayer.stream()
                  .flatMap(d -> effectivePom(d).dependencies().stream())
                  .filter(t -> !visited.contains(t.coordinates()))
                  .filter(
                      t -> !versionlessDirectDependencies.contains(t.coordinates().hideVersion()))
                  .filter(t -> !exclusions.contains(MavenDependency.Exclusion.matching(t)))
                  .filter(this::isTransitiveScope)
                  .filter(this::isNotOptional)
                  .peek(t -> exclusions.addAll(t.exclusions()))
                  .peek(t -> visited.add(t.coordinates()))
                  .collect(Collectors.toList());
          var currentDepth = depth;
          found.addAll(
              transitiveDeps.stream()
                  .map(d -> new DependencyWithDepth(d, currentDepth))
                  .collect(Collectors.toList()));
          nextLayer = transitiveDeps;
        }

        return found;
      } finally {
        span.finish();
      }
    }
  }

  // According to the maven spec, the provided, system and test scope are not transitive. See:
  // https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Scope,
  private boolean isTransitiveScope(MavenDependency dependency) {
    return !dependency.hasScope("test")
        && !dependency.hasScope("provided")
        && !dependency.hasScope("system");
  }

  private boolean isNotOptional(MavenDependency dependency) {
    return !dependency.optional();
  }

  private FlatPom effectivePom(MavenDependency dependency) {
    var pom = getPomMergedWithParentPoms(dependency);

    // According to the maven documentation, managed dependencies with scope "import" should be
    // replaced with the effective list of dependencies in the specified POM's
    // <dependencyManagement> section. See:
    // https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Scope
    var managedDepsToAdd =
        pom.managedDependencies().stream()
            .filter(d -> d.hasScope("import"))
            .map(this::effectivePom)
            .flatMap(p -> p.managedDependencies().stream())
            .collect(Collectors.toList());
    pom.merge(FlatPom.builder().managedDependencies(managedDepsToAdd).build());

    return pom;
  }

  private FlatPom getPomMergedWithParentPoms(MavenDependency dependency) {
    try {
      var location = resolver.resolve(dependency);
      var pom = MavenPomLoader.load(location.pom).pom;
      while (pom.hasParent()) {
        var parent = pom.maybeParent().get();
        var parentLocation = resolver.resolve(parent.coordinates);
        var parentPom = MavenPomLoader.load(parentLocation.pom).pom;
        pom.merge(parentPom);
      }

      return pom;
    } catch (Exception e) {
      if (options.debug()) {
        log.log(Level.WARNING, "Cannot get merged pom for " + dependency, e);
      }

      return FlatPom.builder().build();
    }
  }
}
