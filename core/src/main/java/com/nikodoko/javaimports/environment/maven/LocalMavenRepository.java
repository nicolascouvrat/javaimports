package com.nikodoko.javaimports.environment.maven;

import com.nikodoko.javaimports.Options;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  public List<MavenDependency> getTransitiveDependencies(List<MavenDependency> directDependencies) {
    return getTransitiveDependencies(directDependencies, Optional.empty());
  }

  public List<MavenDependency> getTransitiveDependencies(MavenDependency target, int maxDepth) {
    var directDependencies = getDependencies(target);
    var transitiveDeps =
        getTransitiveDependencies(
            directDependencies, maxDepth == -1 ? Optional.empty() : Optional.of(maxDepth));
    return Stream.concat(directDependencies.stream(), transitiveDeps.stream())
        .collect(Collectors.toList());
  }

  private List<MavenDependency> getTransitiveDependencies(
      List<MavenDependency> directDependencies, Optional<Integer> maxDepth) {
    System.out.println("STARTING " + directDependencies);
    var start = System.currentTimeMillis();
    var toDig = directDependencies;
    var versionless =
        directDependencies.stream().map(MavenDependency::hideVersion).collect(Collectors.toSet());
    var all = new ArrayList<MavenDependency>();
    var depth = 0;
    while (!toDig.isEmpty()) {
      if (maxDepth.isPresent() && maxDepth.get() <= depth) {
        break;
      }

      List<MavenDependency> next = new ArrayList<>();
      depth += 1;
      for (var dep : toDig) {
        System.out.println("DIGGING " + dep);
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

          System.out.println("FOUND " + transitiveDep);
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
    return all;
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
            .map(this::getPomMergedWithParentPoms)
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

  private List<MavenDependency> getDependencies(MavenDependency dependency) {
    try {
      var location = resolver.resolve(dependency);
      var pom = MavenPomLoader.load(location.pom).pom;
      while (pom.hasParent()) {
        var parent = pom.maybeParent().get();
        var parentLocation = resolver.resolve(parent.coordinates);
        var parentPom = MavenPomLoader.load(parentLocation.pom).pom;
        pom.merge(parentPom);
      }

      var scopeImport =
          pom.managedDependencies().stream()
              .filter(d -> d.scope().map(s -> s.equals("import")).orElse(false))
              .collect(Collectors.toList());
      for (var toImport : scopeImport) {
        System.out.println("IMPORTING: " + toImport);
        var toImportLocation = resolver.resolve(toImport);
        var toImportPom = MavenPomLoader.load(toImportLocation.pom).pom;
        pom.merge(toImportPom);
      }

      return pom.dependencies();
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
