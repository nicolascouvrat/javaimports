package com.nikodoko.javaimports.environment.shared;

import static com.google.common.truth.Truth.assertThat;
import static com.nikodoko.javaimports.common.CommonTestUtil.aSelector;

import com.nikodoko.packagetest.BuildSystem;
import com.nikodoko.packagetest.Export;
import com.nikodoko.packagetest.Exported;
import com.nikodoko.packagetest.Module;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ProjectParserTest {
  Exported project;

  @AfterEach
  void cleanup() throws Exception {
    project.cleanup();
  }

  @Test
  void itShouldFindFilesOfSamePackageInDifferentFolder() throws Exception {
    var module =
        Module.named("test.module")
            .containing(
                Module.file("Main.java", "package test.module; public class Main {}"),
                // same package despite being in a different folder
                Module.file("second/Second.java", "package test.module; public class Second {}"));
    project = Export.of(BuildSystem.MAVEN, module);

    var parser =
        new ProjectParser(
            aSelector("test.module"),
            srcs("test.module", "Main.java", "second/Second.java"),
            Runnable::run);
    var got = parser.parseAll();

    assertThat(got.errors()).isEmpty();
    assertThat(got.project().filesInPackage("test.module")).hasSize(2);
    assertThat(got.project().allFiles()).hasSize(2);
  }

  @Test
  void itShouldDoItsBestDespiteErrors() throws Exception {
    var module =
        Module.named("test.module")
            .containing(
                Module.file("Main.java", "package test.module; public class Main {}"),
                Module.file("Other.java", "package test.module; public class Other {}"),
                Module.file("Invalid.java", "this is not valid java code"),
                Module.file(
                    "second/Second.java", "package test.module.second; public class Second {}"));
    project = Export.of(BuildSystem.MAVEN, module);

    var parser =
        new ProjectParser(
            aSelector("test.module"),
            srcs("test.module", "Main.java", "Other.java", "Invalid.java", "second/Second.java"),
            Runnable::run);
    var got = parser.parseAll();

    assertThat(got.errors()).hasSize(1);
    assertThat(got.project().filesInPackage("test.module")).hasSize(2);
    assertThat(got.project().allFiles()).hasSize(3);
  }

  private SourceFiles srcs(String module, String... fragments) {
    var srcs = new ArrayList<Path>();
    for (var f : fragments) {
      project.file(module, f).map(srcs::add);
    }

    return () -> srcs;
  }
}
