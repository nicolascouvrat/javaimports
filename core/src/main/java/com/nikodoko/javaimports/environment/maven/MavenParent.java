package com.nikodoko.javaimports.environment.maven;

import com.nikodoko.javaimports.common.Utils;
import java.nio.file.Path;
import java.util.Optional;

public class MavenParent {
  final MavenCoordinates coordinates;
  final Optional<Path> maybeRelativePath;

  MavenParent(MavenCoordinates coordinates, Optional<Path> maybeRelativePath) {
    this.coordinates = coordinates;
    this.maybeRelativePath = maybeRelativePath;
  }

  @Override
  public String toString() {
    return Utils.toStringHelper(this)
        .add("coordinates", coordinates)
        .add("maybeRelativePath", maybeRelativePath)
        .toString();
  }
}
