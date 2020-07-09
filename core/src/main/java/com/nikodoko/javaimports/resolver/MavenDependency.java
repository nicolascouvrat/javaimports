package com.nikodoko.javaimports.resolver;

import java.nio.file.Path;
import java.nio.file.Paths;

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
}
