package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

public class FlatPomTest {
  @Test
  void itPreservesDependenciesIfItHasNothingToEnrichThemWith() {
    var deps =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", null),
            new MavenDependency("com.nikodoko", "javapackagetest", "${javapackagetest.version}"));

    var pom = FlatPom.builder().dependencies(deps).build();
    assertThat(pom.dependencies()).containsExactlyElementsIn(deps);
    assertThat(pom.isWellDefined()).isFalse();
  }

  @Test
  void itGetsVersionFromManagedDependencyIfNeeded() {
    var deps =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", null),
            new MavenDependency("com.nikodoko", "javapackagetest", "${javapackagetest.version}"));
    var managedDeps =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", "${javaimports.version}"),
            new MavenDependency("com.nikodoko", "javapackagetest", "1.0.0"));
    var expected =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", "${javaimports.version}"),
            new MavenDependency("com.nikodoko", "javapackagetest", "${javapackagetest.version}"));

    var pom = FlatPom.builder().dependencies(deps).managedDependencies(managedDeps).build();
    assertThat(pom.dependencies()).containsExactlyElementsIn(expected);
    assertThat(pom.isWellDefined()).isFalse();
  }

  @Test
  void itSubstitutesPropertiesIfNeeded() {
    var deps =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", "${javaimports.version}"),
            new MavenDependency("com.nikodoko", "javapackagetest", "${javapackagetest.version}"));
    var properties = new Properties();
    properties.setProperty("javaimports.version", "2.0.0");
    var expected =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", "2.0.0"),
            new MavenDependency("com.nikodoko", "javapackagetest", "${javapackagetest.version}"));

    var pom = FlatPom.builder().dependencies(deps).properties(properties).build();
    assertThat(pom.dependencies()).containsExactlyElementsIn(expected);
    assertThat(pom.isWellDefined()).isFalse();
  }

  @Test
  void itCompletelyEnrichesDependenciesIfPossible() {
    var deps =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", null),
            new MavenDependency("com.nikodoko", "javapackagetest", "${javapackagetest.version}"));
    var managedDeps =
        List.of(new MavenDependency("com.nikodoko", "javaimports", "${javaimports.version}"));
    var properties = new Properties();
    properties.setProperty("javaimports.version", "2.0.0");
    properties.setProperty("javapackagetest.version", "1.0.0");
    var expected =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", "2.0.0"),
            new MavenDependency("com.nikodoko", "javapackagetest", "1.0.0"));

    var pom =
        FlatPom.builder()
            .dependencies(deps)
            .managedDependencies(managedDeps)
            .properties(properties)
            .build();
    assertThat(pom.dependencies()).containsExactlyElementsIn(expected);
    assertThat(pom.isWellDefined()).isTrue();
  }

  @Test
  void itDoesNothingWhenMergingIntoAWellDefinedPom() {
    var wellDefined =
        FlatPom.builder()
            .dependencies(List.of(new MavenDependency("com.nikodoko", "javaimports", "1.0.0")))
            .build();
    var other =
        FlatPom.builder()
            .managedDependencies(
                List.of(new MavenDependency("com.nikodoko", "javaimports", "2.0.0")))
            .build();

    wellDefined.merge(other);
    assertThat(wellDefined.isWellDefined()).isTrue();
    assertThat(wellDefined.dependencies())
        .containsExactly(new MavenDependency("com.nikodoko", "javaimports", "1.0.0"));
  }

  @Test
  void itMergesPomsInTheRightOrder() {
    var deps =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", null),
            new MavenDependency("com.nikodoko", "javapackagetest", null));
    var managedDeps =
        List.of(new MavenDependency("com.nikodoko", "javaimports", "${javaimports.version}"));
    var properties = new Properties();
    properties.setProperty("javapackagetest.version", "1.0.0");
    var firstPom =
        FlatPom.builder()
            .dependencies(deps)
            .managedDependencies(managedDeps)
            .properties(properties)
            .build();

    assertThat(firstPom.isWellDefined()).isFalse();

    managedDeps =
        List.of(
            new MavenDependency("com.nikodoko", "javapackagetest", "${javapackagetest.version}"));
    properties = new Properties();
    properties.setProperty("javaimports.version", "2.0.0");
    var secondPom =
        FlatPom.builder().managedDependencies(managedDeps).properties(properties).build();
    var expected =
        List.of(
            new MavenDependency("com.nikodoko", "javaimports", "2.0.0"),
            new MavenDependency("com.nikodoko", "javapackagetest", "1.0.0"));

    firstPom.merge(secondPom);
    assertThat(firstPom.isWellDefined()).isTrue();
    assertThat(firstPom.dependencies()).containsExactlyElementsIn(expected);
  }
}
