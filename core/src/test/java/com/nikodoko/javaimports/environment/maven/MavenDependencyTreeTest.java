package com.nikodoko.javaimports.environment.maven;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MavenDependencyTreeTest {
  static final URL rootURL =
      MavenDependencyTreeTest.class.getResource("/fixtures/unittests/indexation-pom/indexation");
  static Path root;

  @BeforeEach
  void setup() throws Exception {
    root = Paths.get(rootURL.toURI());
  }

  @Test
  void test() throws Exception {
    var dependencies = new MavenDependencyFinder().findAll(root).dependencies;
    System.out.println(dependencies);
    new MavenDependencyTree().getTransitiveDependencies(dependencies);
  }
}
