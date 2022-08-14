package com.nikodoko.javaimports.environment.maven;

import com.nikodoko.javaimports.common.Utils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/** Encapsulates a Maven dependency. */
class MavenDependency {
  static class Exclusion {
    private final String groupId;
    private final String artifactId;

    Exclusion(String groupId, String artifactId) {
      this.groupId = groupId;
      this.artifactId = artifactId;
    }

    static Exclusion matching(MavenDependency dependency) {
      return new Exclusion(dependency.groupId(), dependency.artifactId());
    }

    boolean matches(MavenDependency dependency) {
      return dependency.groupId().equals(groupId) && dependency.artifactId().equals(artifactId);
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }

      if (!(o instanceof Exclusion)) {
        return false;
      }

      var that = (Exclusion) o;
      return Objects.equals(this.groupId, that.groupId)
          && Objects.equals(this.artifactId, that.artifactId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.groupId, this.artifactId);
    }

    @Override
    public String toString() {
      return Utils.toStringHelper(this)
          .add("groupId", this.groupId)
          .add("artifactId", this.artifactId)
          .toString();
    }
  }

  private final boolean optional;
  private final Optional<String> scope;
  private final MavenCoordinates coordinates;
  private final List<Exclusion> exclusions;

  MavenDependency(
      String groupId,
      String artifactId,
      String version,
      String type,
      String classifier,
      String scope,
      boolean optional,
      List<Exclusion> exclusions) {
    this.coordinates = new MavenCoordinates(groupId, artifactId, version, type, classifier);
    this.scope = Optional.ofNullable(scope);
    this.optional = optional;
    this.exclusions = exclusions;
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

  List<Exclusion> exclusions() {
    return exclusions;
  }

  void substitute(Properties props) {
    coordinates.substitute(props);
  }

  boolean hasVersion() {
    return coordinates.hasVersion();
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.coordinates, this.scope, this.optional, this.exclusions);
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
        && Objects.equals(d.exclusions, exclusions)
        && Objects.equals(d.optional, optional);
  }

  @Override
  public String toString() {
    return Utils.toStringHelper(this)
        .add("coordinates", coordinates)
        .add("scope", scope)
        .add("exclusions", exclusions)
        .add("optional", optional)
        .toString();
  }
}
