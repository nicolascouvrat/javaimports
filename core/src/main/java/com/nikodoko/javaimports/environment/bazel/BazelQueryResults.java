package com.nikodoko.javaimports.environment.bazel;

import com.nikodoko.javaimports.environment.shared.SourceFiles;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Parsed result of `bazel query deps(...)` */
public record BazelQueryResults(List<Path> srcs, List<Path> deps) implements SourceFiles {
  public static BazelQueryResults parse(Path workspaceRoot, Path outputBase, Reader reader)
      throws IOException {
    try (var r = new BufferedReader(reader)) {
      var srcs = new ArrayList<Path>();
      var deps = new ArrayList<>();
      String line;
      while ((line = r.readLine()) != null) {
        var parsed = parse(workspaceRoot, outputBase, line);
        if (parsed == null) {
          continue;
        }

        if (parsed.srcPath() != null) {
          srcs.add(parsed.srcPath());
        }
      }

      return new BazelQueryResults(srcs, List.of());
    }
  }

  @Override
  public List<Path> get() {
    return srcs;
  }

  private record ParsedLine(Path srcPath, Path depPath) {}

  // This will match files outside of the package we're considering, but it's fine because we depend
  // directly on their source files so it makes sense to consider as part of the package.
  private static final Pattern SRC_FILE_PATTERN =
      Pattern.compile("^//(?<package>.+):(?<path>.+)\\.java$");
  private static final Pattern DEPENDENCY_PATTERN =
      Pattern.compile("^@maven//:(?<coordinates>.+)\\.jar$");

  private static ParsedLine parse(Path workspaceRoot, Path outputBase, String raw) {
    var srcMatch = SRC_FILE_PATTERN.matcher(raw);
    if (srcMatch.matches()) {
      return new ParsedLine(
          workspaceRoot
              .resolve(srcMatch.group("package"))
              .resolve(srcMatch.group("path") + ".java"),
          null);
    }

    var depMatch = DEPENDENCY_PATTERN.matcher(raw);
    if (depMatch.matches()) {
      var coord = Paths.get(depMatch.group("coordinates") + ".jar");
      return new ParsedLine(null, coord);
    }

    return null;
  }
}
