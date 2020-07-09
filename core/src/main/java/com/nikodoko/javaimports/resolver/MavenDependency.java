package com.nikodoko.javaimports.resolver;

import com.google.common.base.MoreObjects;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class MavenDependency {
  final String groupId;
  final String artifactId;
  final String version;

  MavenDependency(String groupId, String artifactId, String version) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  Path getLocation() {
    return Paths.get(groupId.replace("\\.", "/"), artifactId, version);
  }

  boolean isComplete() {
    return groupId != null && artifactId != null && version != null;
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
