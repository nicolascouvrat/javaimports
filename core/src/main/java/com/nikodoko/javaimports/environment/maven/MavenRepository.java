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
}
