package com.nikodoko.javaimports.environment.maven;

import java.util.List;

/**
 * A {@code MavenRepository} is an abstraction over a maven repository, providing convenient query
 * methods.
 */
public interface MavenRepository {
  /**
   * Gets the resolved list of managed dependencies for the given {@code dependency}. This will
   * check all parents poms for information and resolve any scope "import" along the way.
   */
  List<MavenDependency> getManagedDependencies(MavenDependency dependency);

  /**
   * Gets the transitive dependencies for a set of {@code dependencies}. Similarly to `mvn
   * dependency:tree`, this will resolve version conflicts and return only one version per
   * transitive dependency. This will find dependencies recursively up to {@code maxDepth}.
   */
  List<MavenDependency> getTransitiveDependencies(List<MavenDependency> dependencies, int maxDepth);
}
