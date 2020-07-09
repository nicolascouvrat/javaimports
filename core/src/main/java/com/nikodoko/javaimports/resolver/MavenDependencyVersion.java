package com.nikodoko.javaimports.resolver;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenDependencyVersion {
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
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", name).add("number", number).toString();
  }
}
