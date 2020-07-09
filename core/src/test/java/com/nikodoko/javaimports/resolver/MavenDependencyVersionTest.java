package com.nikodoko.javaimports.resolver;

import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MavenDependencyVersionTest {
  @Test
  void testCanHandleExoticVersioning() {
    List<String> versions = ImmutableList.of("18.0-jre", "17.0", "27.0.1-android");
    List<MavenDependencyVersion> expected =
        ImmutableList.of(
            new MavenDependencyVersion("18.0-jre", "18.0"),
            new MavenDependencyVersion("17.0", "17.0"),
            new MavenDependencyVersion("27.0.1-android", "27.0.1"));

    for (int i = 0; i < versions.size(); i++) {
      assertThat(MavenDependencyVersion.of(versions.get(i))).hasValue(expected.get(i));
    }
  }
}
