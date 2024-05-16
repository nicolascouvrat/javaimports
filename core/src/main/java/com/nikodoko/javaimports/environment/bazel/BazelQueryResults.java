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
  public static BazelQueryResults parse(Path targetRoot, Path workspaceRoot, Reader reader)
      throws IOException {
    try (var r = new BufferedReader(reader)) {
      var srcs = new ArrayList<Path>();
      var deps = new ArrayList<>();
      String line;
      while ((line = r.readLine()) != null) {
        var parsed = parse(targetRoot, workspaceRoot, line);
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

  private static final Pattern SRC_FILE_PATTERN = Pattern.compile("^//.+:(?<path>.+)\\.java$");
  private static final Pattern DEPENDENCY_PATTERN =
      Pattern.compile("^@maven//:(?<coordinates>.+)\\.jar$");

  private static ParsedLine parse(Path targetRoot, Path workspaceRoot, String raw) {
    var srcMatch = SRC_FILE_PATTERN.matcher(raw);
    if (srcMatch.matches()) {
      return new ParsedLine(targetRoot.resolve(Paths.get(srcMatch.group("path") + ".java")), null);
    }

    var depMatch = DEPENDENCY_PATTERN.matcher(raw);
    if (depMatch.matches()) {
      var coord = Paths.get(depMatch.group("coordinates") + ".jar");
      return new ParsedLine(null, coord);
    }

    return null;
  }
}
