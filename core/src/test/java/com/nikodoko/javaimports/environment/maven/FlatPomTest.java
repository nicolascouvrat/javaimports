package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;

public class FlatPomTest {
  static MavenDependency dependency(
      String groupId,
      String artifactId,
      String version,
      String type,
      String scope,
      boolean optional) {
    return new MavenDependency(groupId, artifactId, version, type, null, scope, optional);
  }

  static MavenDependency dependency(String groupId, String artifactId, String version) {
    return dependency(groupId, artifactId, version, "jar", "compile", false);
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
            dependency("com.nikodoko", "javaimports", "1.0.0", "jar", "compile", false),
            dependency("com.nikodoko", "javaimports", "1.0.0", "jar", "test", false));
    var pom = FlatPom.builder().managedDependencies(managedDeps).build();
    assertThat(pom.dependencies()).isEmpty();
  }

  @Test
  void itGetsOptionalFromManagedDependencyIfNeeded() {
    var deps =
        List.of(
            dependency("com.nikodoko", "javaimports", "1.0.0", "jar", "compile", true),
            dependency("com.nikodoko", "javapackagetest", "1.0.0", "jar", "compile", false));
    var managedDeps =
        List.of(
            dependency("com.nikodoko", "javaimports", "1.0.0", "jar", "compile", false),
            dependency("com.nikodoko", "javapackagetest", "1.0.0", "jar", "compile", true));
    var expected =
        List.of(
            dependency("com.nikodoko", "javaimports", "1.0.0", "jar", "compile", true),
            dependency("com.nikodoko", "javapackagetest", "1.0.0", "jar", "compile", true));

    var pom = FlatPom.builder().dependencies(deps).managedDependencies(managedDeps).build();
    assertThat(pom.dependencies()).containsExactlyElementsIn(expected);
  }

  @Test
  void itGetsScopeFromManagedDependencyIfNeeded() {
    var deps =
        List.of(
            dependency("com.nikodoko", "javaimports", "1.0.0", "jar", "compile", false),
            dependency("com.nikodoko", "javapackagetest", "1.0.0", "jar", null, false));
    var managedDeps =
        List.of(
            dependency("com.nikodoko", "javaimports", "1.0.0", "jar", "test", false),
            dependency("com.nikodoko", "javapackagetest", "1.0.0", "jar", "test", false));
    var expected =
        List.of(
            dependency("com.nikodoko", "javaimports", "1.0.0", "jar", "compile", false),
            dependency("com.nikodoko", "javapackagetest", "1.0.0", "jar", "test", false));

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
  void itSubstitutesPropertiesInManagedDependencies() {
    var managedDeps =
        List.of(dependency("com.nikodoko", "javapackagetest", "${javapackagetest.version}"));
    var parentProps = new Properties();
    parentProps.setProperty("javapackagetest.version", "1.0");

    var pom = FlatPom.builder().managedDependencies(managedDeps).build();
    var parentPom = FlatPom.builder().properties(parentProps).build();
    pom.merge(parentPom);

    var expected = List.of(dependency("com.nikodoko", "javapackagetest", "1.0"));
    assertThat(pom.managedDependencies()).containsExactlyElementsIn(expected);
  }

  @Test
  void itSubstitutesPropertiesOutsideOfVersion() {
    var dep =
        dependency(
            "com.typesafe.akka",
            "akka-cluster_${akka-scala.version}",
            "${com.typesafe.akka.akka-cluster.version}");
    var properties = new Properties();
    properties.setProperty("com.typesafe.akka.akka-cluster.version", "${akka.version}");
    properties.setProperty("akka-scala.version", "${scala.version}");
    properties.setProperty("scala.version", "2.12");
    properties.setProperty("akka.version", "2.5.32");
    var expected = dependency("com.typesafe.akka", "akka-cluster_2.12", "2.5.32");

    var pom = FlatPom.builder().dependencies(List.of(dep)).properties(properties).build();
    assertThat(pom.dependencies()).containsExactly(expected);
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
                        new MavenCoordinates(
                            "com.nikodoko", "javaimports-parent", "1.0.0", "pom", null),
                        Optional.of(Paths.get("../pom.xml")))))
            .build();
    var secondPom = FlatPom.builder().maybeParent(Optional.empty()).build();

    firstPom.merge(secondPom);
    assertThat(firstPom.maybeParent().isEmpty()).isTrue();
  }
}
