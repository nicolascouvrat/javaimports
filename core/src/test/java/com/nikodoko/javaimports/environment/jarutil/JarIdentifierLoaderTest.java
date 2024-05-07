package com.nikodoko.javaimports.environment.jarutil;

import static com.google.common.truth.Truth.assertThat;
import static com.nikodoko.javaimports.common.CommonTestUtil.anImport;
import static com.nikodoko.javaimports.common.CommonTestUtil.someIdentifiers;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JarIdentifierLoaderTest {
  static final URL repositoryURL = JarIdentifierLoaderTest.class.getResource("/.m2/repository");
  Path repository;

  @BeforeEach
  void setup() throws Exception {
    repository = Paths.get(repositoryURL.toURI());
  }

  @Test
  void itShouldLoadSubclass() {
    var jar =
        repository.resolve("com/mycompany/app/another-dependency/1.0/another-dependency-1.0.jar");
    var expected =
        someIdentifiers(
            // The class' own identifiers
            "aSubclassProtectedField",
            "aSubclassProtectedMethod",
            "aSubclassPublicField",
            "aSubclassPublicMethod",
            "aSubclassPublicStaticField",
            "aSubclassPublicStaticMethod",
            // The Object class' identifiers
            "clone",
            "equals",
            "finalize",
            "hashCode",
            "notify",
            "notifyAll",
            "toString",
            "wait",
            "getClass");

    var got =
        new JarIdentifierLoader(jar)
            .loadIdentifiers(anImport("com.mycompany.app.another.Parent.AnotherPublicClass"));

    assertThat(got).containsExactlyElementsIn(expected);
  }

  @Test
  void itShouldLoadPublicAndProtectedIdentifiersOfAClassAndItsParents() {
    var jar =
        repository.resolve("com/mycompany/app/another-dependency/1.0/another-dependency-1.0.jar");
    var expected =
        someIdentifiers(
            // The class' own identifiers
            "aProtectedField",
            "aProtectedMethod",
            "aPublicField",
            "aPublicMethod",
            "aPublicStaticField",
            "aPublicStaticMethod",
            "APublicClass",
            "AProtectedClass",
            // Its parent's
            "anotherProtectedField",
            "anotherProtectedMethod",
            "anotherPublicField",
            "anotherPublicMethod",
            "anotherPublicStaticField",
            "anotherPublicStaticMethod",
            "AnotherPublicClass",
            "AnotherProtectedClass",
            // The Object class' identifiers
            "clone",
            "equals",
            "finalize",
            "hashCode",
            "notify",
            "notifyAll",
            "toString",
            "wait",
            "getClass");

    var got = new JarIdentifierLoader(jar).loadIdentifiers(anImport("com.mycompany.app.App"));

    assertThat(got).containsExactlyElementsIn(expected);
  }
}
