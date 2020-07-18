package com.nikodoko.javaimports.resolver;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

class PackageDistanceTest {
  @Test
  void testDistanceToSamePackage() {
    PackageDistance distance = PackageDistance.from("com.test.package");
    int got = distance.to("com.test.package");

    assertThat(got).isEqualTo(0);
  }

  @Test
  void testDistanceToChildPackage() {
    PackageDistance distance = PackageDistance.from("com.test.package");
    int got = distance.to("com.test.package.subpackage");

    assertThat(got).isEqualTo(1);
  }

  @Test
  void testDistanceToParentPackage() {
    PackageDistance distance = PackageDistance.from("com.test.package");
    int got = distance.to("com.test");

    assertThat(got).isEqualTo(1);
  }

  @Test
  void testDistanceToPackageWithNoCommonRoot() {
    PackageDistance distance = PackageDistance.from("com.test.package");
    int got = distance.to("net.different.package");

    assertThat(got).isEqualTo(6);
  }
}
