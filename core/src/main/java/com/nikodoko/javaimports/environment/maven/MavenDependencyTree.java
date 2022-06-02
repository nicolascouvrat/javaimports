package com.nikodoko.javaimports.environment.maven;

import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import io.opentracing.Span;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MavenDependencyTree {
  private static final Path DEFAULT_REPOSITORY =
      Paths.get(System.getProperty("user.home"), ".m2/repository");

  MavenDependencyResolver resolver;
  Options opts;

  MavenDependencyTree(Options opts) {
    this.resolver = MavenDependencyResolver.withRepository(DEFAULT_REPOSITORY);
    this.opts = opts;
  }

  public List<MavenDependency> getTransitiveDependencies(List<MavenDependency> directDependencies) {
    var span = Traces.createSpan("MavenDependencyTree.getTransitiveDependencies");
    try (var __ = Traces.activate(span)) {
      return getTransitiveDependenciesInstrumented(span, directDependencies);
    } finally {
      span.finish();
    }
  }

  public List<MavenDependency> getTransitiveDependenciesInstrumented(
      Span span, List<MavenDependency> directDependencies) {
    var start = System.currentTimeMillis();
    var toDig = directDependencies;
    var versionless =
        directDependencies.stream().map(MavenDependency::hideVersion).collect(Collectors.toSet());
    var all = new ArrayList<MavenDependency>();
    all.addAll(directDependencies);
    while (!toDig.isEmpty()) {
      var futures =
          toDig.stream()
              .map(
                  d ->
                      CompletableFuture.supplyAsync(
                          () -> getDependencies(span, d), opts.executor()))
              .collect(Collectors.toList());
      CompletableFuture.allOf(futures.stream().toArray(CompletableFuture[]::new)).join();
      var next =
          futures.stream()
              .flatMap(f -> f.join().stream())
              .filter(
                  transitiveDep -> {
                    if (versionless.contains(transitiveDep.hideVersion())) {
                      return false;
                    }

                    if (transitiveDep.scope().isPresent()
                        && (transitiveDep.scope().get().equals("test")
                            || transitiveDep.scope().get().equals("provided"))) {
                      return false;
                    }

                    if (transitiveDep.optional()) {
                      return false;
                    }

                    return true;
                  })
              .collect(Collectors.toList());
      var nextVersionless =
          next.stream().map(MavenDependency::hideVersion).collect(Collectors.toSet());
      versionless.addAll(nextVersionless);
      toDig = next;
      all.addAll(next);
    }

    var end = System.currentTimeMillis();
    return all;
  }

  private List<MavenDependency> getDependencies(Span span, MavenDependency dependency) {
    try (var __ = Traces.activate(span)) {
      var childSpan =
          Traces.createSpan(
              "MavenDependencyTree.getDependencies", new Tag("dependency", dependency));
      try (var ___ = Traces.activate(childSpan)) {
        var location = resolver.resolve(dependency);
        var childPom = MavenPomLoader.load(location.pom).pom;
        if (!childPom.hasParent()) {
          return childPom.dependencies();
        }

        var parent = childPom.maybeParent().get();
        var parentLocation = resolver.resolve(parent.coordinates);
        var parentPom = MavenPomLoader.load(parentLocation.pom).pom;
        childPom.merge(parentPom);
        return childPom.dependencies();

      } finally {
        childSpan.finish();
      }
    } catch (Exception e) {
      return List.of();
    }
  }

  // private boolean hasRelativeParentPath(FlatPom pom) {
  //   return pom.maybeParent().flatMap(p -> p.maybeRelativePath).isPresent();
  // }

  // private Path relativeParentPomPath(FlatPom pom) {
  //   var parent = pom.maybeParent().flatMap(p -> p.maybeRelativePath).get();
  //   if (parent.endsWith(POM)) {
  //     return parent;
  //   }

  //   // Consider that we had a directory, attempt to find a pom in it
  //   return parent.resolve(POM);
  // }
}
