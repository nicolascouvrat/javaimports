package com.nikodoko.javaimports.environment.maven;

import static com.nikodoko.javaimports.common.Utils.checkNotNull;

import com.nikodoko.javaimports.common.Utils;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/** Contains the information required to find an artifact in the repository. */
public class MavenCoordinates {
  static class Versionless {
    final MavenCoordinates wrapped;

    Versionless(MavenCoordinates coordinates) {
      this.wrapped = coordinates;
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
          && Objects.equals(this.wrapped.type, that.wrapped.type)
          && Objects.equals(this.wrapped.artifactId, that.wrapped.artifactId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.wrapped.groupId, this.wrapped.artifactId, this.wrapped.type);
    }

    @Override
    public String toString() {
      return Utils.toStringHelper(this)
          .add("groupId", this.wrapped.groupId)
          .add("artifactId", this.wrapped.artifactId)
          .add("type", this.wrapped.type)
          .toString();
    }
  }

  private final String groupId;
  private final String artifactId;
  private final String type;
  private final Optional<MavenString> version;

  MavenCoordinates(String groupId, String artifactId, String version, String type) {
    checkNotNull(groupId, "maven coordinates does not accept a null groupId");
    checkNotNull(artifactId, "maven coordinates does not accept a null artifactId");
    checkNotNull(artifactId, "maven coordinates does not accept a null type");
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = Optional.ofNullable(version).map(MavenString::new);
    this.type = type;
  }

  String version() {
    return version.map(MavenString::toString).orElse(null);
  }

  String artifactId() {
    return artifactId;
  }

  String groupId() {
    return groupId;
  }

  String type() {
    return type;
  }

  void substitute(Properties props) {
    version.map(
        v -> {
          v.substitute(props);
          return v;
        });
  }

  String propertyReferencedByVersion() {
    if (!hasPropertyReferenceVersion()) {
      throw new IllegalStateException("Version does not reference a property: " + this);
    }

    return version.get().propertyReferences().stream().findFirst().get();
  }

  Set<String> propertyReferences() {
    return version.map(MavenString::propertyReferences).orElse(Set.of());
  }

  boolean hasPropertyReferenceVersion() {
    if (!hasVersion()) {
      return false;
    }

    return version.get().propertyReferences().size() == 1;
  }

  boolean hasVersion() {
    return version.isPresent();
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
  public int hashCode() {
    return Objects.hash(this.groupId, this.artifactId, this.version, this.type);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof MavenCoordinates)) {
      return false;
    }

    MavenCoordinates d = (MavenCoordinates) o;
    return Objects.equals(d.groupId, groupId)
        && Objects.equals(d.artifactId, artifactId)
        && Objects.equals(d.type, type)
        && Objects.equals(d.version, version);
  }

  @Override
  public String toString() {
    return Utils.toStringHelper(this)
        .add("groupId", groupId)
        .add("artifactId", artifactId)
        .add("version", version)
        .add("type", type)
        .toString();
  }
}
