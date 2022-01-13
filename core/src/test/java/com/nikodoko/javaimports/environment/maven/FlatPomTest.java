package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;

public class FlatPomTest {
  static MavenDependency dependency(String groupId, String artifactId, String version) {
    return new MavenDependency(groupId, artifactId, version, "jar", "compile", false);
  }

  @Test
  void itPreservesDependenciesIfItHasNothingToEnrichThemWith() {
    var deps =
        List.of(
            dependency("com.nikodoko", "javaimports", null),
            dependency("com.nikodoko", "javapackagetest", "${javapackagetest.version}"));

    var pom = FlatPom.builder().dependencies(deps).build();
    assertThat(pom.dependencies()).containsExactlyElementsIn(deps);
  }

  @Test
  void itGetsVersionFromManagedDependencyIfNeeded() {
    var deps =
        List.of(
            dependency("com.nikodoko", "javaimports", null),
            dependency("com.nikodoko", "javapackagetest", "${javapackagetest.version}"));
    var managedDeps =
        List.of(
            dependency("com.nikodoko", "javaimports", "${javaimports.version}"),
            dependency("com.nikodoko", "javapackagetest", "1.0.0"));
    var expected =
        List.of(
            dependency("com.nikodoko", "javaimports", "${javaimports.version}"),
            dependency("com.nikodoko", "javapackagetest", "${javapackagetest.version}"));

    var pom = FlatPom.builder().dependencies(deps).managedDependencies(managedDeps).build();
    assertThat(pom.dependencies()).containsExactlyElementsIn(expected);
  }

  @Test
  void itSupportsDependenciesOfDifferentTypeWithTheSameCoordinates() {
    var managedDeps =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", "1.0.0", "jar", "compile", false),
            new MavenDependency("com.nikodoko", "javaimports", "1.0.0", "jar", "test", false));
    var pom = FlatPom.builder().managedDependencies(managedDeps).build();
    assertThat(pom.dependencies()).isEmpty();
  }

  @Test
  void itGetsOptionalFromManagedDependencyIfNeeded() {
    var deps =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", "1.0.0", "jar", "compile", true),
            new MavenDependency(
                "com.nikodoko", "javapackagetest", "1.0.0", "jar", "compile", false));
    var managedDeps =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", "1.0.0", "jar", "compile", false),
            new MavenDependency(
                "com.nikodoko", "javapackagetest", "1.0.0", "jar", "compile", true));
    var expected =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", "1.0.0", "jar", "compile", true),
            new MavenDependency(
                "com.nikodoko", "javapackagetest", "1.0.0", "jar", "compile", true));

    var pom = FlatPom.builder().dependencies(deps).managedDependencies(managedDeps).build();
    assertThat(pom.dependencies()).containsExactlyElementsIn(expected);
  }

  @Test
  void itGetsScopeFromManagedDependencyIfNeeded() {
    var deps =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", "1.0.0", "jar", "compile", false),
            new MavenDependency("com.nikodoko", "javapackagetest", "1.0.0", "jar", null, false));
    var managedDeps =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", "1.0.0", "jar", "test", false),
            new MavenDependency("com.nikodoko", "javapackagetest", "1.0.0", "jar", "test", false));
    var expected =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", "1.0.0", "jar", "compile", false),
            new MavenDependency("com.nikodoko", "javapackagetest", "1.0.0", "jar", "test", false));

    var pom = FlatPom.builder().dependencies(deps).managedDependencies(managedDeps).build();
    assertThat(pom.dependencies()).containsExactlyElementsIn(expected);
  }

  @Test
  void itSubstitutesPropertiesIfNeeded() {
    var deps =
        List.of(
            dependency("com.nikodoko", "javaimports", "${javaimports.version}"),
            dependency("com.nikodoko", "javapackagetest", "${javapackagetest.version}"));
    var properties = new Properties();
    properties.setProperty("javaimports.version", "2.0.0");
    var expected =
        List.of(
            dependency("com.nikodoko", "javaimports", "2.0.0"),
            dependency("com.nikodoko", "javapackagetest", "${javapackagetest.version}"));

    var pom = FlatPom.builder().dependencies(deps).properties(properties).build();
    assertThat(pom.dependencies()).containsExactlyElementsIn(expected);
  }

  @Test
  void itCompletelyEnrichesDependenciesIfPossible() {
    var deps =
        List.of(
            dependency("com.nikodoko", "javaimports", null),
            dependency("com.nikodoko", "javapackagetest", "${javapackagetest.version}"));
    var managedDeps = List.of(dependency("com.nikodoko", "javaimports", "${javaimports.version}"));
    var properties = new Properties();
    properties.setProperty("javaimports.version", "2.0.0");
    properties.setProperty("javapackagetest.version", "1.0.0");
    var expected =
        List.of(
            dependency("com.nikodoko", "javaimports", "2.0.0"),
            dependency("com.nikodoko", "javapackagetest", "1.0.0"));

    var pom =
        FlatPom.builder()
            .dependencies(deps)
            .managedDependencies(managedDeps)
            .properties(properties)
            .build();
    assertThat(pom.dependencies()).containsExactlyElementsIn(expected);
  }

  @Test
  void itDoesNothingWhenMergingIntoAWellDefinedPom() {
    var wellDefined =
        FlatPom.builder()
            .dependencies(List.of(dependency("com.nikodoko", "javaimports", "1.0.0")))
            .build();
    var other =
        FlatPom.builder()
            .managedDependencies(List.of(dependency("com.nikodoko", "javaimports", "2.0.0")))
            .build();

    wellDefined.merge(other);
    assertThat(wellDefined.dependencies())
        .containsExactly(dependency("com.nikodoko", "javaimports", "1.0.0"));
  }

  @Test
  void itMergesPomsInTheRightOrder() {
    var deps =
        List.of(
            dependency("com.nikodoko", "javaimports", null),
            dependency("com.nikodoko", "javapackagetest", null));
    var managedDeps = List.of(dependency("com.nikodoko", "javaimports", "${javaimports.version}"));
    var properties = new Properties();
    properties.setProperty("javapackagetest.version", "1.0.0");
    var firstPom =
        FlatPom.builder()
            .dependencies(deps)
            .managedDependencies(managedDeps)
            .properties(properties)
            .build();

    managedDeps =
        List.of(dependency("com.nikodoko", "javapackagetest", "${javapackagetest.version}"));
    properties = new Properties();
    properties.setProperty("javaimports.version", "2.0.0");
    var secondPom =
        FlatPom.builder().managedDependencies(managedDeps).properties(properties).build();
    var expected =
        List.of(
            dependency("com.nikodoko", "javaimports", "2.0.0"),
            dependency("com.nikodoko", "javapackagetest", "1.0.0"));

    firstPom.merge(secondPom);
    assertThat(firstPom.dependencies()).containsExactlyElementsIn(expected);
  }

  @Test
  void itInheritsParentWhenMerging() {
    var firstPom =
        FlatPom.builder()
            .dependencies(List.of(dependency("com.nikodoko", "javaimports", null)))
            .maybeParent(
                Optional.of(
                    new MavenParent(
                        new MavenCoordinates("com.nikodoko", "javaimports-parent", "1.0.0", "pom"),
                        Optional.of(Paths.get("../pom.xml")))))
            .build();
    var secondPom = FlatPom.builder().maybeParent(Optional.empty()).build();

    firstPom.merge(secondPom);
    assertThat(firstPom.maybeParent().isEmpty()).isTrue();
  }
}
