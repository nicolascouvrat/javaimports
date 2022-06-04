package com.nikodoko.javaimports.environment.maven;

import com.nikodoko.javaimports.common.Utils;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

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
          && Objects.equals(this.wrapped.type(), that.wrapped.type())
          && Objects.equals(this.wrapped.artifactId(), that.wrapped.artifactId());
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.wrapped.groupId(), this.wrapped.artifactId(), this.wrapped.type());
    }

    @Override
    public String toString() {
      return Utils.toStringHelper(this)
          .add("groupId", this.wrapped.groupId())
          .add("artifactId", this.wrapped.artifactId())
          .add("type", this.wrapped.type())
          .toString();
    }
  }

  private final boolean optional;
  private final Optional<String> scope;
  private final MavenCoordinates coordinates;

  MavenDependency(
      String groupId,
      String artifactId,
      String version,
      String type,
      String classifier,
      String scope,
      boolean optional) {
    this.coordinates = new MavenCoordinates(groupId, artifactId, version, type, classifier);
    this.scope = Optional.ofNullable(scope);
    this.optional = optional;
  }

  String type() {
    return coordinates.type();
  }

  Optional<String> scope() {
    return scope;
  }

  // TODO: tentative API
  boolean hasScope(String desired) {
    return scope.map(s -> s.equals(desired)).orElse(false);
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
    return coordinates.maybeVersion().map(MavenString::toString).orElse(null);
  }

  Optional<String> classifier() {
    return coordinates.maybeClassifier();
  }

  void substitute(Properties props) {
    coordinates.substitute(props);
  }

  boolean hasVersion() {
    return coordinates.hasVersion();
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.coordinates, this.scope, this.optional);
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
    return Objects.equals(d.coordinates, coordinates)
        && Objects.equals(d.scope, scope)
        && Objects.equals(d.optional, optional);
  }

  @Override
  public String toString() {
    return Utils.toStringHelper(this)
        .add("coordinates", coordinates)
        .add("scope", scope)
        .add("optional", optional)
        .toString();
  }
}
