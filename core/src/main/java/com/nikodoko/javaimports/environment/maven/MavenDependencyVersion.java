package com.nikodoko.javaimports.environment.maven;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenDependencyVersion implements Comparable<MavenDependencyVersion> {
  // Supports "exotic" versioning, like guava's "26.0-jre"
  private static final Pattern VERSION_REGEX =
      Pattern.compile("(?:\\D+)?(?<versionNumber>\\d+(?:\\.\\d+)+)(?:\\D+)?");
  static final MavenDependencyVersion INVALID = new MavenDependencyVersion("INVALID", "INVALID");

  String name;
  String number;

  MavenDependencyVersion(String name, String number) {
    this.name = name;
    this.number = number;
  }

  static Optional<MavenDependencyVersion> of(String name) {
    Matcher m = VERSION_REGEX.matcher(name);
    if (!m.matches()) {
      return Optional.empty();
    }

    return Optional.of(new MavenDependencyVersion(name, m.group("versionNumber")));
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof MavenDependencyVersion)) {
      return false;
    }

    MavenDependencyVersion v = (MavenDependencyVersion) o;
    return Objects.equals(v.name, name) && Objects.equals(v.number, number);
  }

  @Override
  public int compareTo(MavenDependencyVersion other) {
    String[] numbers = number.split("\\.");
    String[] otherNumbers = other.number.split("\\.");
    int maxLength = Math.max(numbers.length, otherNumbers.length);
    for (int i = 0; i < maxLength; i++) {
      Integer number = i < numbers.length ? Integer.parseInt(numbers[i]) : 0;
      Integer otherNumber = i < otherNumbers.length ? Integer.parseInt(otherNumbers[i]) : 0;
      Integer res = number.compareTo(otherNumber);

      if (res != 0) return res;
    }

    return 0;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", name).add("number", number).toString();
  }
}
