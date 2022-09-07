package com.nikodoko.javaimports.environment.maven;

import java.util.List;
import org.junit.jupiter.api.Test;

public class MavenClassLoaderTest {
  @Test
  void itShouldLoadFromDirectAndTransitiveDependencies() {}

  static class DummyRepository implements MavenRepository {
    private final List<MavenDependency> deps;

    DummyRepository(List<MavenDependency> deps) {
      this.deps = deps;
    }

    @Override
    public List<MavenDependency> getManagedDependencies(MavenDependency d) {
      return List.of();
    }

    @Override
    public List<MavenDependency> getTransitiveDependencies(List<MavenDependency> d, int maxDepth) {
      return deps;
    }
  }
}
