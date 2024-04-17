package com.nikodoko.javaimports.environment.jarutil;

import static com.google.common.truth.Truth.assertThat;
import static com.nikodoko.javaimports.common.CommonTestUtil.aStaticImport;
import static com.nikodoko.javaimports.common.CommonTestUtil.anImport;

import com.nikodoko.javaimports.common.Import;
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

public class LazyJarTest {
  static final URL repositoryURL = JarIdentifierLoaderTest.class.getResource("/testrepository");
  Path repository;

  @BeforeEach
  void setup() throws Exception {
    repository = Paths.get(repositoryURL.toURI());
  }

  @Test
  void itShouldParseAChildClass() {
    var jar =
        repository.resolve("com/mycompany/app/another-dependency/1.0/another-dependency-1.0.jar");

    var got = new LazyJar(jar);
    got.parse(aStaticImport("com.mycompany.app.another.Parent.AnotherPublicClass"));
  }

  @Test
  void itShouldParseAParentClass() {
    var jar =
        repository.resolve("com/mycompany/app/another-dependency/1.0/another-dependency-1.0.jar");

    var got = new LazyJar(jar);
    got.parse(anImport("com.mycompany.app.another.Parent"));
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
    var jar = new LazyJar(repository.resolve(jarPath));
    var got = jar.importables();
    assertThat(got).containsExactlyElementsIn(expected);
  }
}
