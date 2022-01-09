package com.nikodoko.javaimports.environment.maven;

import static com.nikodoko.javaimports.common.Utils.checkNotNull;

import com.nikodoko.javaimports.common.Utils;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Contains the information required to find an artifact in the repository. */
public class MavenCoordinates {
  // static class Versionless {
  //   final MavenCoordinates wrapped;

  //   Versionless(MavenCoordinates coordinates) {
  //     this.wrapped = coordinates;
  //   }

  //   @Override
  //   public boolean equals(Object o) {
  //     if (o == null) {
  //       return false;
  //     }

  //     if (!(o instanceof Versionless)) {
  //       return false;
  //     }

  //     var that = (Versionless) o;
  //     return Objects.equals(this.wrapped.groupId, that.wrapped.groupId)
  //         && Objects.equals(this.wrapped.artifactId(), that.wrapped.artifactId());
  //   }

  //   @Override
  //   public int hashCode() {
  //     return Objects.hash(this.wrapped.groupId, this.wrapped.artifactId, this.wrapped.type);
  //   }

  //   @Override
  //   public String toString() {
  //     return Utils.toStringHelper(this)
  //         .add("groupId", this.wrapped.groupId)
  //         .add("artifactId", this.wrapped.artifactId)
  //         .add("type", this.wrapped.type)
  //         .toString();
  //   }
  // }

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

  private final String groupId;
  private final String artifactId;
  private final Version version;
  private final String type;

  MavenCoordinates(String groupId, String artifactId, String version, String type) {
    checkNotNull(groupId, "maven coordinates does not accept a null groupId");
    checkNotNull(artifactId, "maven coordinates does not accept a null artifactId");
    checkNotNull(artifactId, "maven coordinates does not accept a null type");
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = new Version(version);
    this.type = type;
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

  String type() {
    return type;
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

  // Versionless hideVersion() {
  //   return new Versionless(this);
  // }

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
