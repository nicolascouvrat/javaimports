package com.nikodoko.javaimports.resolver;

import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.packagetest.Export;
import com.nikodoko.packagetest.Exported;
import com.nikodoko.packagetest.Module;
import com.nikodoko.packagetest.exporters.Kind;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class MavenResolverTest {
  @Test
  void testThatTopLevelClassesAreFound() throws Exception {
    Module module =
        new Module(
            "test.module",
            ImmutableMap.of(
                "Main.java",
                "package test.module; public class Main {}",
                "second/Second.java",
                "package test.module.second; public class Second {}"));
    Exported project = Export.of(Kind.MAVEN, ImmutableList.of(module));
    Path target = project.file(module.name(), "Main.java").get();

    Resolver resolver = Resolvers.basedOnEnvironment(target);
    assertThat(resolver.find("Second")).hasValue(new Import("Second", "test.module.second", false));

    project.cleanup();
  }
}
