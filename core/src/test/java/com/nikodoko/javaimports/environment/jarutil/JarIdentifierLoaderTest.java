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
  static final URL repositoryURL = JarIdentifierLoaderTest.class.getResource("/testrepository");
  Path repository;

  @BeforeEach
  void setup() throws Exception {
    repository = Paths.get(repositoryURL.toURI());
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
            // Its parent's
            "anotherProtectedField",
            "anotherProtectedMethod",
            "anotherPublicField",
            "anotherPublicMethod",
            "anotherPublicStaticField",
            "anotherPublicStaticMethod",
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
