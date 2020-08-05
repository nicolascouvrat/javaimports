package com.nikodoko.javaimports.resolver;

import static com.google.common.truth.Truth.assertThat;

import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.resolver.maven.MavenResolver;
import com.nikodoko.packagetest.BuildSystem;
import com.nikodoko.packagetest.Export;
import com.nikodoko.packagetest.Exported;
import com.nikodoko.packagetest.Module;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class ResolversTest {
  @Test
  void testBasedOnEnvironmentInMavenProject() throws Exception {
    Module module =
        Module.named("test.module")
            .containing(
                Module.file("Main.java", "package test.module;"),
                Module.file("second/Second.java", "package test.module.second;"));
    Exported project = Export.of(BuildSystem.MAVEN, module);
    Path target = project.file(module.name(), "Main.java").get();

    Resolver got = Resolvers.basedOnEnvironment(target, "test.module", Options.defaults());
    assertThat(got).isInstanceOf(MavenResolver.class);
    project.cleanup();
  }
}
