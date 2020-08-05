package com.nikodoko.javaimports.resolver.maven;

import static com.google.common.truth.Truth8.assertThat;

import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.resolver.Resolver;
import com.nikodoko.javaimports.resolver.Resolvers;
import com.nikodoko.packagetest.BuildSystem;
import com.nikodoko.packagetest.Export;
import com.nikodoko.packagetest.Exported;
import com.nikodoko.packagetest.Module;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class MavenResolverTest {
  Exported project;

  @AfterEach
  void cleanup() throws Exception {
    project.cleanup();
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
}
