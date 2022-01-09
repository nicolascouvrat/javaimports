package com.nikodoko.javaimports.environment.maven;

import com.nikodoko.javaimports.common.Utils;
import java.util.Objects;
import java.util.Optional;

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
      return Objects.equals(this.wrapped.groupId(), that.wrapped.groupId())
          && Objects.equals(this.wrapped.type, that.wrapped.type)
          && Objects.equals(this.wrapped.artifactId(), that.wrapped.artifactId());
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.wrapped.groupId(), this.wrapped.artifactId(), this.wrapped.type);
    }

    @Override
    public String toString() {
      return Utils.toStringHelper(this)
          .add("groupId", this.wrapped.groupId())
          .add("artifactId", this.wrapped.artifactId())
          .add("type", this.wrapped.type)
          .toString();
    }
  }

  private final String type;
  private final boolean optional;
  private final Optional<String> scope;
  private final MavenCoordinates coordinates;

  MavenDependency(
      String groupId,
      String artifactId,
      String version,
      String type,
      String scope,
      boolean optional) {
    this.coordinates = new MavenCoordinates(groupId, artifactId, version, type);
    this.type = type;
    this.scope = Optional.ofNullable(scope);
    this.optional = optional;
  }

  String type() {
    return coordinates.type();
  }

  Optional<String> scope() {
    return scope;
  }

  boolean optional() {
    return optional;
  }

  MavenCoordinates coordinates() {
    return coordinates;
  }

  Versionless hideVersion() {
    return new Versionless(this);
  }

  // All these are convenience methods calling the corresponding one in MavenCoordinates
  // TODO: maybe get rid of them?
  String artifactId() {
    return coordinates.artifactId();
  }

  String groupId() {
    return coordinates.groupId();
  }

  String version() {
    return coordinates.version();
  }

  String propertyReferencedByVersion() {
    return coordinates.propertyReferencedByVersion();
  }

  boolean hasPropertyReferenceVersion() {
    return coordinates.hasPropertyReferenceVersion();
  }

  boolean hasVersion() {
    return coordinates.hasVersion();
  }

  boolean hasWellDefinedVersion() {
    return coordinates.hasWellDefinedVersion();
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.coordinates, this.type, this.scope, this.optional);
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
    return Objects.equals(d.type, type)
        && Objects.equals(d.coordinates, coordinates)
        && Objects.equals(d.scope, scope)
        && Objects.equals(d.optional, optional);
  }

  @Override
  public String toString() {
    return Utils.toStringHelper(this)
        .add("coordinates", coordinates)
        .add("type", type)
        .add("scope", scope)
        .add("optional", optional)
        .toString();
  }
}
