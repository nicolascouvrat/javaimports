package com.nikodoko.javaimports.environment;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Calculates the distance between two packages.
 *
 * <p>For example, {@code com.a.package} and {@code com.a.package.subpackage} have a distance of 1,
 * {@code com.a.package} and {@code com.a} also have a distance of 1, and {@code com.a.package} and
 * {@code net.another.package} have a distance of 6.
 */
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
