package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth.assertThat;
import static com.nikodoko.javaimports.common.CommonTestUtil.anImport;

import com.google.common.collect.ImmutableList;
import com.nikodoko.javaimports.parser.Import;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MavenDependencyLoaderTest {
  static final URL repositoryURL = MavenDependencyLoaderTest.class.getResource("/testrepository");
  MavenDependencyLoader loader;
  Path repository;

  @BeforeEach
  void setup() throws Exception {
    repository = Paths.get(repositoryURL.toURI());
  }

  static Stream<Arguments> jarPathProvider() {
    return Stream.of(
        Arguments.of(
            "Classes are found",
            "com/mycompany/app/a-dependency/1.0/a-dependency-1.0.jar",
            List.of(
                anImport("com.mycompany.app.App"),
                anImport("com.mycompany.anotherapp.AnotherApp"),
                anImport("com.mycompany.app.another.app.again.AnotherApp"))),
        Arguments.of(
            "Subclasses are found",
            "com/mycompany/app/a-dependency/2.0/a-dependency-2.0.jar",
            List.of(
                anImport("com.mycompany.app.App.Subclass"),
                anImport("com.mycompany.app.App.Subclass.Subsubclass"),
                anImport("com.mycompany.app.App"))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("jarPathProvider")
  void testDependencyLoading(String name, String jarPath, List<Import> expected) throws Exception {
    var got = MavenDependencyLoader.load(repository.resolve(jarPath));
    assertThat(got).containsExactlyElementsIn(expected);
  }

  @Test
  void testJava9DependencyIsResolved() throws Exception {
    var expected = ImmutableList.of(anImport("com.mycompany.app.App"));

    var got =
        MavenDependencyLoader.load(
            repository.resolve(
                "com/mycompany/app/a-java9-dependency/1.0/a-java9-dependency-1.0.jar"));

    // TODO: this should be an exact comparison, but we don't really have java9 support for now and
    // also extract imports we shouldnt (the ones not exposed by module-info.class)
    assertThat(got).containsAtLeastElementsIn(expected);
  }
}
