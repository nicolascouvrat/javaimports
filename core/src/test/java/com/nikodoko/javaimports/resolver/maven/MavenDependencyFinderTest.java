package com.nikodoko.javaimports.resolver.maven;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MavenDependencyFinderTest {
  Path tmp;

  @BeforeEach
  void setup() throws Exception {
    tmp = Files.createTempDirectory("");
  }

  @Test
  void testThatPomWithNoDepenciesReturnsEmptyList() throws Exception {
    MavenDependencyFinder finder = new MavenDependencyFinder();
    write(basicPom());

    MavenDependencyFinder.Result got = finder.findAll(tmp);
    assertThat(got.dependencies).isEmpty();
    assertThat(got.errors).isEmpty();
  }

  @Test
  void testThatPomWithDependenciesReturnsCorrectDependencies() throws Exception {
    MavenDependencyFinder finder = new MavenDependencyFinder();
    write(
        basicPom(),
        withDependencies(
            "com.google.guava", "guava", "28.1-jre", "com.google.truth", "truth", "1.0.1"));
    List<MavenDependency> expected =
        ImmutableList.of(
            new MavenDependency("com.google.guava", "guava", "28.1-jre"),
            new MavenDependency("com.google.truth", "truth", "1.0.1"));
    MavenDependencyFinder.Result got = finder.findAll(tmp);
    assertThat(got.dependencies).containsExactlyElementsIn(expected);
    assertThat(got.errors).isEmpty();
  }

  @Test
  void testDependenciesUsingParametersAreFound() throws Exception {
    MavenDependencyFinder finder = new MavenDependencyFinder();
    write(basicPom(), withDependencies("com.google.guava", "guava", "${guava.version}"));
    List<MavenDependency> expected =
        ImmutableList.of(new MavenDependency("com.google.guava", "guava", "${guava.version}"));

    MavenDependencyFinder.Result got = finder.findAll(tmp);
    assertThat(got.dependencies).containsExactlyElementsIn(expected);
    assertThat(got.errors).isEmpty();
  }

  @Test
  void testThatFinderDoesNotCrashOnInvalidPom() throws Exception {
    Files.write(Paths.get(tmp.toString(), "pom.xml"), "this is not a valid pom!".getBytes());

    MavenDependencyFinder.Result got = new MavenDependencyFinder().findAll(tmp);
    assertThat(got.dependencies).isEmpty();
    assertThat(got.errors).hasSize(1);
  }

  static Consumer<Model> basicPom() {
    return m -> {
      m.setModelVersion("4.0.0");
      m.setGroupId("com.nikodoko.javaimports");
      m.setArtifactId("test-pom");
      m.setVersion("0.0");
    };
  }

  static Consumer<Model> withDependencies(String... elements) {
    List<Dependency> deps = new ArrayList<>();
    for (int i = 0; i < elements.length; i = i + 3) {
      Dependency dep = new Dependency();
      dep.setGroupId(elements[i]);
      dep.setArtifactId(elements[i + 1]);
      dep.setVersion(elements[i + 2]);
      deps.add(dep);
    }

    return m -> m.setDependencies(deps);
  }

  void write(Consumer<Model>... options) throws Exception {
    Model pom = new Model();
    for (Consumer<Model> opt : options) {
      opt.accept(pom);
    }

    File target = Paths.get(tmp.toString(), "pom.xml").toFile();
    new DefaultModelWriter().write(target, null, pom);
  }
}
