package com.nikodoko.javaimports.environment.maven;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Encapsulates a Maven dependency. */
class MavenDependency {
  /**
   * {@code Versionless} provides a convenient way to compare dependencies while ignoring their
   * versions.
   */
  static class Versionless {
    final MavenDependency wrapped;

    Versionless(MavenDependency dependency) {
      this.wrapped = dependency;
    }

    MavenDependency showVersion() {
      return wrapped;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }

      if (!(o instanceof Versionless)) {
        return false;
      }

      var that = (Versionless) o;
      return Objects.equals(this.wrapped.groupId, that.wrapped.groupId)
          && Objects.equals(this.wrapped.artifactId, that.wrapped.artifactId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.wrapped.groupId, this.wrapped.artifactId);
    }
  }

  private static class Version {
    private static final Pattern PATTERN = Pattern.compile("\\$\\{(?<parameter>\\S+)\\}");

    final String value;
    // Empty if value is not a property reference
    final Optional<String> property;

    Version(String value) {
      this.value = value;
      this.property = maybeExtractProperty(value);
    }

    private Optional<String> maybeExtractProperty(String value) {
      if (value == null) {
        return Optional.empty();
      }

      var m = PATTERN.matcher(value);
      if (!m.matches()) {
        return Optional.empty();
      }

      return Optional.of(m.group("parameter"));
    }

    @Override
    public String toString() {
      return value;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }

      if (!(o instanceof Version)) {
        return false;
      }

      var that = (Version) o;
      return Objects.equals(this.value, that.value) && Objects.equals(this.property, that.property);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.value, this.property);
    }
  }

  private static final Pattern parameterPattern = Pattern.compile("\\$\\{(?<parameter>\\S+)\\}");
  private final String groupId;
  private final String artifactId;
  private final Version version;

  MavenDependency(String groupId, String artifactId, String version) {
    checkNotNull(groupId, "maven dependency does not accept a null groupId");
    checkNotNull(artifactId, "maven dependency does not accept a null artifactId");
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = new Version(version);
  }

  String version() {
    return version.value;
  }

  String artifactId() {
    return artifactId;
  }

  String groupId() {
    return groupId;
  }

  String propertyReferencedByVersion() {
    if (!hasPropertyReferenceVersion()) {
      throw new IllegalStateException("Version does not reference a property: " + this);
    }

    return version.property.get();
  }

  boolean hasPropertyReferenceVersion() {
    return version.property.isPresent();
  }

  boolean hasVersion() {
    return version.value != null;
  }

  /**
   * A version is assumed to be well defined if it exists and is not a property reference.
   *
   * <p>We don't handle cases where the version is simply invalid.
   */
  boolean hasWellDefinedVersion() {
    return hasVersion() && !hasPropertyReferenceVersion();
  }

  Versionless hideVersion() {
    return new Versionless(this);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof MavenDependency)) {
      return false;
    }

    MavenDependency d = (MavenDependency) o;
    return Objects.equals(d.groupId, groupId)
        && Objects.equals(d.artifactId, artifactId)
        && Objects.equals(d.version, version);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("groupId", groupId)
        .add("artifactId", artifactId)
        .add("version", version)
        .toString();
  }
}
