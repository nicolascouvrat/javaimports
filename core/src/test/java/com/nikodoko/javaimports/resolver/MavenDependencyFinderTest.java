package com.nikodoko.javaimports.resolver;

import static com.google.common.truth.Truth.assertThat;

import java.io.StringReader;
import org.junit.jupiter.api.Test;

public class MavenDependencyFinderTest {
  @Test
  void testThatPomWithNoDepenciesReturnsEmptyList() throws Exception {
    MavenDependencyFinder finder = new MavenDependencyFinder();
    String[] pom =
        new String[] {
          "<project>",
          "<modelVersion>4.0.0</modelVersion>",
          "<groupId>com.nikodoko.javaimports</groupId>",
          "<artifactId>javaimports-parent</artifactId>",
          "<packaging>pom</packaging>",
          "<version>0.2-SNAPSHOT</version>",
          "</project>",
        };
    try (StringReader reader = new StringReader(String.join("\n", pom) + "\n")) {
      finder.scan(reader);
    }

    assertThat(finder.allFound()).isTrue();
    assertThat(finder.result()).hasSize(0);
  }
}
