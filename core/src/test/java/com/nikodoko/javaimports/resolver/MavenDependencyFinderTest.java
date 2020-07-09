package com.nikodoko.javaimports.resolver;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MavenDependencyFinderTest {
  @Test
  void testThatPomWithNoDepenciesReturnsEmptyList() throws Exception {
    MavenDependencyFinder finder = new MavenDependencyFinder();
    String[] pom =
        new String[] {
          "<project>",
          " <modelVersion>4.0.0</modelVersion>",
          " <groupId>com.nikodoko.javaimports</groupId>",
          " <artifactId>javaimports-parent</artifactId>",
          " <packaging>pom</packaging>",
          " <version>0.2-SNAPSHOT</version>",
          "</project>",
        };
    try (StringReader reader = getReader(pom)) {
      finder.scan(reader);
    }

    assertThat(finder.allFound()).isTrue();
    assertThat(finder.result()).hasSize(0);
  }

  @Test
  void testThatPomWithDependenciesReturnsCorrectDependencies() throws Exception {
    MavenDependencyFinder finder = new MavenDependencyFinder();
    String[] pom =
        new String[] {
          "<project>",
          " <modelVersion>4.0.0</modelVersion>",
          " <groupId>com.nikodoko.javaimports</groupId>",
          " <artifactId>javaimports-parent</artifactId>",
          " <packaging>pom</packaging>",
          " <version>0.2-SNAPSHOT</version>",
          " <dependencies>",
          "   <dependency>",
          "     <groupId>com.google.guava</groupId>",
          "     <artifactId>guava</artifactId>",
          "     <version>28.1-jre</version>",
          "   </dependency>",
          "   <dependency>",
          "     <groupId>com.google.truth</groupId>",
          "     <artifactId>truth</artifactId>",
          "     <version>1.0.1</version>",
          "   </dependency>",
          " </dependencies>",
          "</project>",
        };
    List<MavenDependency> expected =
        ImmutableList.of(
            new MavenDependency("com.google.guava", "guava", "28.1-jre"),
            new MavenDependency("com.google.truth", "truth", "1.0.1"));
    try (StringReader reader = getReader(pom)) {
      finder.scan(reader);
    }

    assertThat(finder.allFound()).isTrue();
    assertThat(finder.result()).containsExactlyElementsIn(expected);
  }

  private static StringReader getReader(String[] pom) {
    return new StringReader(String.join("\n", pom) + "\n");
  }
}
