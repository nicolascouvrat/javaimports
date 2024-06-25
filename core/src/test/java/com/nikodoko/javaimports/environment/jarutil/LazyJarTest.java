package com.nikodoko.javaimports.environment.jarutil;

import static com.google.common.truth.Truth.assertThat;
import static com.nikodoko.javaimports.common.CommonTestUtil.aSelector;
import static com.nikodoko.javaimports.common.CommonTestUtil.anImport;
import static com.nikodoko.javaimports.common.CommonTestUtil.someIdentifiers;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Superclass;
import com.nikodoko.javaimports.common.telemetry.Logs;
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
  static final URL repositoryURL = JarIdentifierLoaderTest.class.getResource("/.m2/repository");
  Path repository;

  @BeforeEach
  void setup() throws Exception {
    Logs.enable();
    repository = Paths.get(repositoryURL.toURI());
  }

  @Test
  void itShouldParseAChildClass() {
    var expected =
        ClassEntity.named(aSelector("com.mycompany.app.App"))
            .extending(Superclass.resolved(anImport("com.mycompany.app.another.Parent")))
            .declaring(
                someIdentifiers(
                    "aPublicMethod",
                    "aProtectedMethod",
                    "aPublicStaticField",
                    "aPublicStaticMethod",
                    "aPublicField",
                    "aProtectedField",
                    "AProtectedClass",
                    "APublicClass",
                    "<init>"))
            .build();
    var path =
        repository.resolve("com/mycompany/app/another-dependency/1.0/another-dependency-1.0.jar");
    var jar = new LazyJar(path);

    var got = jar.findClass(anImport("com.mycompany.app.App"));

    assertThat(got.isPresent()).isTrue();
    assertThat(got.get()).isEqualTo(expected);
  }

  @Test
  void itShouldParseADeeplyNestedClass() {
    var expected =
        ClassEntity.named(
                aSelector("com.mycompany.app.another.Parent.AnotherProtectedClass.Nest1.Nest2"))
            .extending(Superclass.resolved(anImport("java.lang.Object")))
            .declaring(someIdentifiers("<init>"))
            .build();
    var path =
        repository.resolve("com/mycompany/app/another-dependency/1.0/another-dependency-1.0.jar");
    var jar = new LazyJar(path);

    var got =
        jar.findClass(
            anImport("com.mycompany.app.another.Parent.AnotherProtectedClass.Nest1.Nest2"));

    assertThat(got.isPresent()).isTrue();
    assertThat(got.get()).isEqualTo(expected);
  }

  @Test
  void itShouldParseANestedClass() {
    var expected =
        ClassEntity.named(aSelector("com.mycompany.app.another.Parent.AnotherPublicClass"))
            .extending(Superclass.resolved(anImport("java.lang.Object")))
            .declaring(
                someIdentifiers(
                    "aSubclassProtectedField",
                    "aSubclassPublicField",
                    "aSubclassPublicMethod",
                    "aSubclassProtectedMethod",
                    "aSubclassPublicStaticMethod",
                    "aSubclassPublicStaticField",
                    "<init>"))
            .build();
    var path =
        repository.resolve("com/mycompany/app/another-dependency/1.0/another-dependency-1.0.jar");
    var jar = new LazyJar(path);

    var got = jar.findClass(anImport("com.mycompany.app.another.Parent.AnotherPublicClass"));

    assertThat(got.isPresent()).isTrue();
    assertThat(got.get()).isEqualTo(expected);
  }

  @Test
  void itShouldParseAParentClass() {
    var expected =
        ClassEntity.named(aSelector("com.mycompany.app.another.Parent"))
            .extending(Superclass.resolved(anImport("java.lang.Object")))
            .declaring(
                someIdentifiers(
                    "anotherPublicStaticField",
                    "anotherProtectedMethod",
                    "anotherProtectedField",
                    "anotherPublicMethod",
                    "anotherPublicField",
                    "anotherPublicStaticMethod",
                    "AnotherProtectedClass",
                    "AnotherPublicClass",
                    "ArrayList",
                    "<init>"))
            .build();
    var path =
        repository.resolve("com/mycompany/app/another-dependency/1.0/another-dependency-1.0.jar");

    var jar = new LazyJar(path);

    var got = jar.findClass(anImport("com.mycompany.app.another.Parent"));

    assertThat(got.isPresent()).isTrue();
    assertThat(got.get()).isEqualTo(expected);
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
