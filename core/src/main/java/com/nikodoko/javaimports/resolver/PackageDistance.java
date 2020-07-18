package com.nikodoko.javaimports.resolver;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PackageDistance {
  private final Path reference;

  private PackageDistance(Path reference) {
    this.reference = reference;
  }

  public static PackageDistance from(String pkg) {
    return new PackageDistance(toPath(pkg));
  }

  public int to(String pkg) {
    Path to = toPath(pkg);
    return distance(reference, to);
  }

  private static Path toPath(String pkg) {
    return Paths.get(pkg.replace(".", "/"));
  }

  private static int distance(Path from, Path to) {
    if (from.equals(to)) {
      return 0;
    }

    return from.relativize(to).getNameCount();
  }
}
