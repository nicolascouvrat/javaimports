package com.nikodoko.javaimports.resolver;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.parser.Import;
import java.nio.file.Path;
import java.util.Objects;

class ImportWithDistance implements Comparable<ImportWithDistance> {
  final Import i;
  // The distance to an imports is defined as the length of the relative path between the file
  // being resolved and the directory containing this import.
  final int distance;

  ImportWithDistance(Import i, int distance) {
    this.i = i;
    this.distance = distance;
  }

  static ImportWithDistance of(Import i, Path in, Path from) {
    return new ImportWithDistance(i, distance(from, in));
  }

  private static int distance(Path from, Path to) {
    return from.relativize(to).toString().split("/").length;
  }

  @Override
  public int compareTo(ImportWithDistance other) {
    if (other.distance == distance) {
      return 0;
    }

    if (distance > other.distance) {
      return 1;
    }

    return -1;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof ImportWithDistance)) {
      return false;
    }

    ImportWithDistance other = (ImportWithDistance) o;
    return Objects.equals(i, other.i) && distance == other.distance;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("import", i).add("distance", distance).toString();
  }
}
