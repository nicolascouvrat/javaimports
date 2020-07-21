package com.nikodoko.javaimports.resolver.maven;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.nikodoko.packagetest.Export;
import com.nikodoko.packagetest.Exported;
import com.nikodoko.packagetest.Module;
import com.nikodoko.packagetest.exporters.Kind;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class MavenProjectParserTest {
  Exported project;

  @AfterEach
  void cleanup() throws Exception {
    project.cleanup();
  }

  @Test
  void testFindFilesOfSamePackageInDifferentFolder() throws Exception {
    Module module =
        new Module(
            "test.module",
            ImmutableMap.of(
                "Main.java",
                "package test.module; public class Main {}",
                // same package despite being in another folder
                "second/Second.java",
                "package test.module; public class Second {}"));
    project = Export.of(Kind.MAVEN, ImmutableList.of(module));

    MavenProjectParser parser = MavenProjectParser.withRoot(project.root());
    MavenProjectParser.Result got = parser.parseAll();

    assertThat(got.errors).isEmpty();
    assertThat(got.project.filesInPackage("test.module")).hasSize(2);
    assertThat(got.project.allFiles()).hasSize(2);
  }

  @Test
  void testExcludedFilesAreIgnored() throws Exception {
    Module module =
        new Module(
            "test.module",
            ImmutableMap.of(
                "Main.java",
                "package test.module; public class Main {}",
                "second/Second.java",
                "package test.module.second; public class Second {}"));
    project = Export.of(Kind.MAVEN, ImmutableList.of(module));
    Path target = project.file(module.name(), "Main.java").get();

    MavenProjectParser parser = MavenProjectParser.withRoot(project.root()).excluding(target);
    MavenProjectParser.Result got = parser.parseAll();

    assertThat(got.project.filesInPackage("test.module")).isEmpty();
    assertThat(got.project.allFiles()).hasSize(1);
    assertThat(got.errors).isEmpty();
  }

  @Test
  void testDoesItsBestDespiteErrors() throws Exception {
    Module module =
        new Module(
            "test.module",
            ImmutableMap.of(
                "Main.java",
                "package test.module; public class Main {}",
                "Other.java",
                "package test.module; public class Other {}",
                "Invalid.java",
                "this is not valid java code",
                "second/Second.java",
                "package test.module.second; public class Second {}"));
    project = Export.of(Kind.MAVEN, ImmutableList.of(module));
    Path target = project.file(module.name(), "Main.java").get();

    MavenProjectParser parser = MavenProjectParser.withRoot(project.root()).excluding(target);
    MavenProjectParser.Result got = parser.parseAll();

    assertThat(got.project.filesInPackage("test.module")).hasSize(1);
    assertThat(got.project.allFiles()).hasSize(2);
    assertThat(got.errors).hasSize(1);
  }
}
