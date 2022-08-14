package com.nikodoko.javaimports.environment.maven;

import static com.nikodoko.javaimports.common.Utils.checkNotNull;

import com.nikodoko.javaimports.common.Utils;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

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
          && Objects.equals(this.wrapped.classifier, that.wrapped.classifier)
          && Objects.equals(this.wrapped.artifactId, that.wrapped.artifactId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          this.wrapped.groupId,
          this.wrapped.artifactId,
          this.wrapped.type,
          this.wrapped.classifier);
    }

    @Override
    public String toString() {
      return Utils.toStringHelper(this)
          .add("groupId", this.wrapped.groupId)
          .add("artifactId", this.wrapped.artifactId)
          .add("type", this.wrapped.type)
          .add("classifier", this.wrapped.classifier)
          .toString();
    }
  }

  private final MavenString groupId;
  private final MavenString artifactId;
  private final MavenString type;
  private final Optional<MavenString> version;
  private final Optional<MavenString> classifier;

  MavenCoordinates(
      String groupId, String artifactId, String version, String type, String classifier) {
    checkNotNull(groupId, "maven coordinates does not accept a null groupId");
    checkNotNull(artifactId, "maven coordinates does not accept a null artifactId");
    checkNotNull(artifactId, "maven coordinates does not accept a null type");
    this.groupId = new MavenString(groupId);
    this.artifactId = new MavenString(artifactId);
    this.version = Optional.ofNullable(version).map(MavenString::new);
    this.classifier = Optional.ofNullable(classifier).map(MavenString::new);
    this.type = new MavenString(type);
  }

  Optional<String> maybeClassifier() {
    return classifier.map(MavenString::toString);
  }

  Optional<MavenString> maybeVersion() {
    return version;
  }

  String artifactId() {
    return artifactId.toString();
  }

  String groupId() {
    return groupId.toString();
  }

  String type() {
    return type.toString();
  }

  void substitute(Properties props) {
    groupId.substitute(props);
    artifactId.substitute(props);
    type.substitute(props);
    if (version.isPresent()) {
      version.get().substitute(props);
    }
    if (classifier.isPresent()) {
      classifier.get().substitute(props);
    }
  }

  boolean hasVersion() {
    return version.isPresent();
  }

  Versionless hideVersion() {
    return new Versionless(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.groupId, this.artifactId, this.version, this.type, this.classifier);
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
        && Objects.equals(d.classifier, classifier)
        && Objects.equals(d.version, version);
  }

  @Override
  public String toString() {
    return Utils.toStringHelper(this)
        .add("groupId", groupId)
        .add("artifactId", artifactId)
        .add("version", version)
        .add("type", type)
        .add("classifier", classifier)
        .toString();
  }
}
