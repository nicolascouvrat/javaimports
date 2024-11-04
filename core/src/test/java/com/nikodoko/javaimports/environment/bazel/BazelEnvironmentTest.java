package com.nikodoko.javaimports.environment.bazel;

import static com.google.common.truth.Truth.assertThat;
import static com.nikodoko.javaimports.common.CommonTestUtil.aSelector;
import static com.nikodoko.javaimports.common.CommonTestUtil.anImport;

import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.packagetest.BuildSystem;
import com.nikodoko.packagetest.Export;
import com.nikodoko.packagetest.Exported;
import com.nikodoko.packagetest.Module;
import com.nikodoko.packagetest.Repository;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class BazelEnvironmentTest {
  private static final URL repositoryURL =
      BazelEnvironmentTest.class.getResource("/.m2/repository");

  private Exported project;

  @AfterEach
  void cleanup() throws IOException {
    project.cleanup();
  }

  @Test
  void itShouldProgressivelyExposeImports() throws Exception {
    var localRepo = Paths.get(repositoryURL.toURI());
    var localRepoUrl = "file://%s".formatted(localRepo);
    var tertiaryModule =
        Module.named("tertiary.module")
            .containing(
                Module.file(
                    "A.java",
                    """
                    package tertiary.module;

                    public class A {
                      public static class ANested {}
                    }
                    """));
    var secondaryModule =
        Module.named("secondary.module")
            .containing(
                Module.file(
                    "A.java",
                    """
                    package secondary.module;

                    public class A {
                      public static class ANested {}
                    }
                    """))
            .dependingOn(Module.dependency("com.mycompany.app", "another-dependency", "1.0"))
            .dependingOn(tertiaryModule);
    var mainModule =
        Module.named("main.module")
            .containing(
                Module.file("Main.java", "package main.module;"),
                Module.file(
                    "A.java",
                    """
                    package main.module;

                    public class A {
                      public static class ANested {}
                    }
                    """))
            .dependingOn(Module.dependency("com.mycompany.app", "a-dependency", "1.0"))
            .dependingOn(secondaryModule);
    Repository repoLocal = Repository.named("local").at(localRepoUrl);
    Repository repoCentral = Repository.named("central").at("https://repo1.maven.org/maven2");
    project =
        Export.of(
            BuildSystem.BAZEL,
            List.of(repoLocal, repoCentral),
            List.of(mainModule, secondaryModule, tertiaryModule));
    var env =
        new BazelEnvironment(
            project.root(),
            project.root().resolve("mainmodule"),
            true,
            project.file("main.module", "Main.java").get(),
            aSelector("main.module"),
            Options.defaults());

    // At first, for files:
    // - we can see main.module.A and secondary.module.A because they are direct dependencies AND
    // can be inferrred from name.
    // - we cannot see tertiary.module.A because it's a transitive dep
    // - we cannot see any *.ANested because we have not parsed anything
    // At first, for jars:
    // - we can't see anything for AnotherApp (coming from a-dependency) despite it's
    // direct because direct JARs have not been loaded
    // - we can't see anything for Parent (coming from another-dependency) because it's
    // transitive
    assertThat(env.findImports(new Identifier("A")))
        .containsExactly(anImport("main.module.A"), anImport("secondary.module.A"));
    assertThat(env.findImports(new Identifier("ANested"))).isEmpty();
    assertThat(env.findImports(new Identifier("AnotherApp"))).isEmpty();
    assertThat(env.findImports(new Identifier("Parent"))).isEmpty();

    assertThat(env.increasePrecision()).isTrue();

    // The only change is that direct JARs are parsed
    assertThat(env.findImports(new Identifier("A")))
        .containsExactly(anImport("main.module.A"), anImport("secondary.module.A"));
    assertThat(env.findImports(new Identifier("ANested"))).isEmpty();
    assertThat(env.findImports(new Identifier("AnotherApp")))
        .containsExactly(
            anImport("com.mycompany.anotherapp.AnotherApp"),
            anImport("com.mycompany.app.another.app.again.AnotherApp"));
    assertThat(env.findImports(new Identifier("Parent"))).isEmpty();

    assertThat(env.increasePrecision()).isTrue();

    // The only change is that we have parsed direct files
    assertThat(env.findImports(new Identifier("A")))
        .containsExactly(anImport("main.module.A"), anImport("secondary.module.A"));
    assertThat(env.findImports(new Identifier("ANested")))
        .containsExactly(anImport("main.module.A.ANested"), anImport("secondary.module.A.ANested"));
    assertThat(env.findImports(new Identifier("AnotherApp")))
        .containsExactly(
            anImport("com.mycompany.anotherapp.AnotherApp"),
            anImport("com.mycompany.app.another.app.again.AnotherApp"));
    assertThat(env.findImports(new Identifier("Parent"))).isEmpty();

    assertThat(env.increasePrecision()).isTrue();

    // Now we can see everything except the ANested coming from tertiary.module as we've loaded
    // transitive JARs and added name-inferrence for transitive deps
    assertThat(env.findImports(new Identifier("A")))
        .containsExactly(
            anImport("main.module.A"),
            anImport("secondary.module.A"),
            anImport("tertiary.module.A"));
    assertThat(env.findImports(new Identifier("ANested")))
        .containsExactly(anImport("main.module.A.ANested"), anImport("secondary.module.A.ANested"));
    assertThat(env.findImports(new Identifier("AnotherApp")))
        .containsExactly(
            anImport("com.mycompany.anotherapp.AnotherApp"),
            anImport("com.mycompany.app.another.app.again.AnotherApp"));
    assertThat(env.findImports(new Identifier("Parent")))
        .contains(anImport("com.mycompany.app.another.Parent"));

    // Last stage
    assertThat(env.increasePrecision()).isTrue();

    // Everything is visible
    assertThat(env.findImports(new Identifier("A")))
        .containsExactly(
            anImport("main.module.A"),
            anImport("secondary.module.A"),
            anImport("tertiary.module.A"));
    assertThat(env.findImports(new Identifier("ANested")))
        .containsExactly(
            anImport("main.module.A.ANested"),
            anImport("secondary.module.A.ANested"),
            anImport("tertiary.module.A.ANested"));
    assertThat(env.findImports(new Identifier("AnotherApp")))
        .containsExactly(
            anImport("com.mycompany.anotherapp.AnotherApp"),
            anImport("com.mycompany.app.another.app.again.AnotherApp"));
    assertThat(env.findImports(new Identifier("Parent")))
        .contains(anImport("com.mycompany.app.another.Parent"));

    assertThat(env.increasePrecision()).isFalse();
  }
}
