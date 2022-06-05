package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

public class MavenDependencyTest {
  @Test
  void testVersionlessEquals() {
    var a =
        new MavenDependency("com.test", "dep", "12.0", "jar", null, "compile", false, List.of());
    var b =
        new MavenDependency("com.test", "dep", "14.0", "jar", null, "compile", false, List.of());
    assertThat(a.hideVersion()).isEqualTo(b.hideVersion());
  }
}
