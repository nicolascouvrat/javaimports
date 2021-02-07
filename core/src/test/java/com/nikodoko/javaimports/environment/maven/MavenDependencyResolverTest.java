package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth8.assertThat;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MavenDependencyResolverTest {
  static final URL repositoryURL = MavenDependencyLoaderTest.class.getResource("/testrepository");
  MavenDependencyResolver resolver;
  Path repository;

  static Stream<Arguments> dependencyProvider() {
    return Stream.of(
        Arguments.of(
            "A dependency with a plain version is found",
            new MavenDependency("com.mycompany.app", "a-dependency", "1.0"),
            "com/mycompany/app/a-dependency/1.0/a-dependency-1.0.jar"),
        Arguments.of(
            "A dependency without plain version is resolved to latest",
            new MavenDependency("com.mycompany.app", "a-dependency", null),
            "com/mycompany/app/a-dependency/2.0/a-dependency-2.0.jar"));
  }

  @BeforeEach
  void setup() throws Exception {
    repository = Paths.get(repositoryURL.toURI());
    resolver = MavenDependencyResolver.withRepository(repository);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("dependencyProvider")
  void testResolveJar(String name, MavenDependency dependency, String relativePath)
      throws Exception {
    var got = resolver.resolveJar(dependency);
    assertThat(got).isEqualTo(repository.resolve(relativePath));
  }
}
