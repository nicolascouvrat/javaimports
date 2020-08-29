package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth8.assertThat;

import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.environment.Resolver;
import com.nikodoko.javaimports.environment.Resolvers;
import com.nikodoko.packagetest.BuildSystem;
import com.nikodoko.packagetest.Export;
import com.nikodoko.packagetest.Exported;
import com.nikodoko.packagetest.Module;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MavenEnvironmentTest {
  static final URL repositoryURL = MavenDependencyLoaderTest.class.getResource("/testrepository");
  Exported project;
  Path repository;

  @AfterEach
  void cleanup() throws Exception {
    project.cleanup();
  }

  @BeforeEach
  void setup() throws Exception {
    repository = Paths.get(repositoryURL.toURI());
  }

  @Test
  void testThatTopLevelClassesAreFound() throws Exception {
    Module module =
        Module.named("test.module")
            .containing(
                Module.file("Main.java", "package test.module; public class Main {}"),
                Module.file(
                    "second/Second.java", "package test.module.second; public class Second {}"));
    project = Export.of(BuildSystem.MAVEN, module);
    Path target = project.file(module.name(), "Main.java").get();

    Resolver resolver = Resolvers.basedOnEnvironment(target, "test.module", Options.defaults());
    assertThat(resolver.find("Second")).hasValue(new Import("Second", "test.module.second", false));
  }

  @Test
  void testThatFileBeingResolvedIsNotFound() throws Exception {
    Module module =
        Module.named("test.module")
            .containing(
                Module.file("Main.java", "package test.module; public class Main {}"),
                Module.file(
                    "second/Second.java", "package test.module.second; public class Second {}"));
    project = Export.of(BuildSystem.MAVEN, module);
    Path target = project.file(module.name(), "Main.java").get();

    Resolver resolver = Resolvers.basedOnEnvironment(target, "test.module", Options.defaults());
    assertThat(resolver.find("Main")).isEmpty();
  }

  @Test
  void testThatFindPicksTheClosestImport() throws Exception {
    Module module =
        Module.named("test.module")
            .containing(
                Module.file("Main.java", "package test.module; public class Main {}"),
                Module.file(
                    "second/Second.java", "package test.module.second; public class Second {}"),
                Module.file(
                    "other/second/Second.java",
                    "package test.module.other.second; public class Second {}"));
    project = Export.of(BuildSystem.MAVEN, module);
    Path target = project.file(module.name(), "Main.java").get();

    Resolver resolver = Resolvers.basedOnEnvironment(target, "test.module", Options.defaults());
    assertThat(resolver.find("Second")).hasValue(new Import("Second", "test.module.second", false));
  }

  @Test
  void testThatDependenciesAreFound() throws Exception {
    Module module =
        Module.named("test.module")
            .containing(Module.file("Main.java", "package test.module;"))
            .dependingOn(Module.dependency("com.mycompany.app", "a-dependency", "1.0"));
    project = Export.of(BuildSystem.MAVEN, module);
    Path target = project.file(module.name(), "Main.java").get();
    Resolver resolver =
        Resolvers.basedOnEnvironment(
            target, "test.module", Options.builder().repository(repository).build());

    // Assert that the 1.0 version of the dependency is indeed selected by checking that a class
    // only present in 2.0 is not found
    // TODO: enable
    assertThat(resolver.find("App")).hasValue(new Import("App", "com.mycompany.app", false));
    // assertThat(resolver.find("Subclass")).isEmpty();
  }
}
