package com.nikodoko.javaimports.environment.jarutil;

import static com.nikodoko.javaimports.common.CommonTestUtil.aStaticImport;
import static com.nikodoko.javaimports.common.CommonTestUtil.anImport;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JarParserTest {
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

    var got = new JarParser(jar);
    got.parse(aStaticImport("com.mycompany.app.another.Parent.AnotherPublicClass"));
  }

  @Test
  void itShouldParseAParentClass() {
    var jar =
        repository.resolve("com/mycompany/app/another-dependency/1.0/another-dependency-1.0.jar");

    var got = new JarParser(jar);
    got.parse(anImport("com.mycompany.app.another.Parent"));
  }
}
