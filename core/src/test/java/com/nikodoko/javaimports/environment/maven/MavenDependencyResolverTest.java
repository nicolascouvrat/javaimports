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
            "A dependency with a classifier is found",
            mavenDependency("com.mycompany.app", "a-dependency", "1.0", "native"),
            "com/mycompany/app/a-dependency/1.0/a-dependency-1.0-native.jar",
            "com/mycompany/app/a-dependency/1.0/a-dependency-1.0.pom"),
        Arguments.of(
            "A dependency with a plain version is found",
            mavenDependency("com.mycompany.app", "a-dependency", "1.0"),
            "com/mycompany/app/a-dependency/1.0/a-dependency-1.0.jar",
            "com/mycompany/app/a-dependency/1.0/a-dependency-1.0.pom"),
        Arguments.of(
            "A dependency with a test-jar type is found",
            new MavenDependency(
                "com.mycompany.app", "a-dependency", "1.0", "test-jar", null, "test", false),
            "com/mycompany/app/a-dependency/1.0/a-dependency-1.0-tests.jar",
            "com/mycompany/app/a-dependency/1.0/a-dependency-1.0.pom"),
        Arguments.of(
            "A dependency with an unresolved property is resolved to the first available one",
            mavenDependency("com.mycompany.app", "a-dependency", "${a.property}"),
            "com/mycompany/app/a-dependency/1.0/a-dependency-1.0.jar",
            "com/mycompany/app/a-dependency/1.0/a-dependency-1.0.pom"),
        Arguments.of(
            "A dependency without plain version is resolved to the first available one",
            mavenDependency("com.mycompany.app", "a-dependency", null),
            "com/mycompany/app/a-dependency/1.0/a-dependency-1.0.jar",
            "com/mycompany/app/a-dependency/1.0/a-dependency-1.0.pom"));
  }

  @BeforeEach
  void setup() throws Exception {
    repository = Paths.get(repositoryURL.toURI());
    resolver = MavenDependencyResolver.withRepository(repository);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("dependencyProvider")
  void testResolveJar(String name, MavenDependency dependency, String jarPath, String pomPath)
      throws Exception {
    var got = resolver.resolve(dependency);
    assertThat(got.jar).isEqualTo(repository.resolve(jarPath));
    assertThat(got.pom).isEqualTo(repository.resolve(pomPath));
  }

  static MavenDependency mavenDependency(String groupId, String artifactId, String version) {
    return mavenDependency(groupId, artifactId, version, null);
  }

  static MavenDependency mavenDependency(
      String groupId, String artifactId, String version, String classifier) {
    return new MavenDependency(groupId, artifactId, version, "jar", classifier, "compile", false);
  }
}
