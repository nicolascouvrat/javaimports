package com.nikodoko.javaimports.environment.maven;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MavenDependencyTree {
  private static final Path DEFAULT_REPOSITORY =
      Paths.get(System.getProperty("user.home"), ".m2/repository");

  MavenDependencyResolver resolver;

  MavenDependencyTree() {
    this.resolver = MavenDependencyResolver.withRepository(DEFAULT_REPOSITORY);
  }

  public List<MavenDependency> getTransitiveDependencies(List<MavenDependency> directDependencies) {
    var toDig = directDependencies;
    var versionless =
        directDependencies.stream().map(MavenDependency::hideVersion).collect(Collectors.toSet());
    while (!toDig.isEmpty()) {
      System.out.println("NEXT LAYER");
      List<MavenDependency> next = new ArrayList<>();
      for (var dep : toDig) {
        System.out.println("DIGGING " + dep);
        List<MavenDependency> forDep = new ArrayList<>();
        var transitiveDeps = getDependencies(dep);
        for (var transitiveDep : transitiveDeps) {
          if (versionless.contains(transitiveDep.hideVersion())) {
            continue;
          }

          forDep.add(transitiveDep);
          versionless.add(transitiveDep.hideVersion());
        }
        forDep.stream().forEach(System.out::println);
        next.addAll(forDep);
      }
      toDig = next;
    }

    return List.of();
  }

  private List<MavenDependency> getDependencies(MavenDependency dependency) {
    try {
      var location = resolver.resolve(dependency);
      return MavenPomLoader.load(location.pom).pom.dependencies();
    } catch (Exception e) {
      return List.of();
    }
  }
}
