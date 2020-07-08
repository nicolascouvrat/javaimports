package com.nikodoko.javaimports.resolver;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.nikodoko.packagetest.Export;
import com.nikodoko.packagetest.Exported;
import com.nikodoko.packagetest.Module;
import com.nikodoko.packagetest.exporters.Kind;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class ResolversTest {
  @Test
  void testBasedOnEnvironmentInMavenProject() throws Exception {
    Module module =
        new Module(
            "test.module",
            ImmutableMap.of(
                "Main.java",
                "package test.module;",
                "second/Second.java",
                "package test.module.second;"));
    Exported project = Export.of(Kind.MAVEN, ImmutableList.of(module));
    Path target = project.file(module.name(), "Main.java").get();

    Resolver got = Resolvers.basedOnEnvironment(target);
    assertThat(got).isInstanceOf(MavenResolver.class);
    project.cleanup();
  }
}
