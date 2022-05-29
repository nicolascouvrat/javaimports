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
    var start = System.currentTimeMillis();
    var toDig = directDependencies;
    var versionless =
        directDependencies.stream().map(MavenDependency::hideVersion).collect(Collectors.toSet());
    var all = new ArrayList<MavenDependency>();
    all.addAll(directDependencies);
    while (!toDig.isEmpty()) {
      List<MavenDependency> next = new ArrayList<>();
      for (var dep : toDig) {
        List<MavenDependency> forDep = new ArrayList<>();
        var transitiveDeps = getDependencies(dep);
        for (var transitiveDep : transitiveDeps) {
          if (versionless.contains(transitiveDep.hideVersion())) {
            continue;
          }

          if (transitiveDep.scope().isPresent()
              && (transitiveDep.scope().get().equals("test")
                  || transitiveDep.scope().get().equals("provided"))) {
            continue;
          }

          if (transitiveDep.optional()) {
            continue;
          }

          forDep.add(transitiveDep);
          versionless.add(transitiveDep.hideVersion());
        }
        next.addAll(forDep);
      }
      toDig = next;
      all.addAll(next);
    }

    var end = System.currentTimeMillis();
    System.out.println(all.size());
    System.out.println(end - start);
    return List.of();
  }

  private List<MavenDependency> getDependencies(MavenDependency dependency) {
    try {
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
