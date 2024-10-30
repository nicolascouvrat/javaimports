package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth.assertThat;

import com.nikodoko.javaimports.environment.shared.Dependency;
import com.nikodoko.packagetest.BuildSystem;
import com.nikodoko.packagetest.Export;
import com.nikodoko.packagetest.Exported;
import com.nikodoko.packagetest.Module;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class MavenProjectFinderTest {
  Exported project;

  @AfterEach
  void cleanup() throws Exception {
    project.cleanup();
  }

  @Test
  void itShouldExcludeFiles() throws Exception {
    var module =
        Module.named("test.module")
            .containing(
                Module.file("Main.java", "package test.module; public class Main {}"),
                Module.file(
                    "second/Second.java", "package test.module.second; public class Second {}"));
    project = Export.of(BuildSystem.MAVEN, module);
    var target = project.file(module.name(), "Main.java").get();

    var finder = MavenProjectFinder.withRoot(project.root()).exclude(target);
    var got = finder.srcs().stream().map(Dependency::path).toList();

    assertThat(got).containsExactly(project.file("test.module", "second/Second.java").get());
  }
}
