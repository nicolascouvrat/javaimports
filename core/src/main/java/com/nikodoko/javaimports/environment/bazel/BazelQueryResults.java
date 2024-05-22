package com.nikodoko.javaimports.environment.bazel;

import com.nikodoko.javaimports.common.Utils;
import com.nikodoko.javaimports.environment.shared.SourceFiles;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parsed result of `bazel query deps(...)` */
record BazelQueryResults(List<Path> srcs, List<Path> deps) implements SourceFiles {
  static Parser parser() {
    return new Parser();
  }

  static class Parser {
    private Boolean isModule;
    private Path workspaceRoot;
    private Path outputBase;

    Parser isModule(boolean isModule) {
      this.isModule = isModule;
      return this;
    }

    Parser workspaceRoot(Path workspaceRoot) {
      this.workspaceRoot = workspaceRoot;
      return this;
    }

    Parser outputBase(Path outputBase) {
      this.outputBase = outputBase;
      return this;
    }

    BazelQueryResults parse(Reader reader) throws IOException {
      Utils.checkNotNull(isModule, "call isModule() before parse()");
      Utils.checkNotNull(workspaceRoot, "call workspaceRoot() before parse()");
      Utils.checkNotNull(outputBase, "call outputBase() before parse()");

      var external = outputBase.resolve("external");
      try (var r = new BufferedReader(reader)) {
        var srcs = new ArrayList<Path>();
        var deps = new ArrayList<Path>();
        String line;
        while ((line = r.readLine()) != null) {
          var srcMatch = SRC_FILE_PATTERN.matcher(line);
          if (srcMatch.matches()) {
            srcs.add(
                workspaceRoot
                    .resolve(srcMatch.group("package"))
                    .resolve(srcMatch.group("path") + ".java"));
            continue;
          }

          var dep = DependencyPatterns.tryMatch(external, isModule, line);
          if (dep != null) {
            deps.add(dep);
            continue;
          }
        }

        return new BazelQueryResults(srcs, deps);
      }
    }
  }

  @Override
  public List<Path> get() {
    return srcs;
  }

  // This will match files outside of the package we're considering, but it's fine because we depend
  // directly on their source files so it makes sense to consider as part of the package.
  private static final Pattern SRC_FILE_PATTERN =
      Pattern.compile("^//(?<package>.+):(?<path>.+)\\.java$");

  // Depending on the build setup (MODULE, WORKSPACE, pinned dependencies or not) the shape of jar
  // dependencies changes, and their location too.
  private static enum DependencyPatterns {
    // For MODULE, and pin, we ingore dependencies starting with @maven//:... but we match lines
    // like @@rules_jvm_external~~maven~xx_yy//file:.../zzz.jar
    MODULE_WITH_PIN(
        Pattern.compile("^@@rules_jvm_external~~maven~(?<name>\\w+)//file:(?<path>.+)\\.jar$"),
        (base, m) ->
            base.resolve("rules_jvm_external~~maven~" + m.group("name"))
                .resolve("file")
                .resolve(m.group("path") + ".jar")),
    // For MODULE, and no pin, we match dependencies starting with @maven//:v1/...
    // That "v1" is part of the path, but we have to filter on this to differentiate from the pinned
    // case where @maven//:... should be ignored.
    MODULE_NO_PIN(
        Pattern.compile("^@maven//:v1/(?<path>.+)\\.jar$"),
        (base, m) ->
            base.resolve("rules_jvm_external~~maven~maven")
                .resolve("v1")
                .resolve(m.group("path") + ".jar")),
    // Workspace and no pin is matched on the same pattern as module but maps to a different
    // directory
    WORKSPACE_NO_PIN(
        Pattern.compile("^@maven//:v1/(?<path>.+)\\.jar$"),
        (base, m) -> base.resolve("maven").resolve("v1").resolve(m.group("path") + ".jar")),
    // For WORKSPACE, and pin, we ingore dependencies starting with @maven//:... but we match lines
    // like @xx_yy//file:.../zzz.jar
    WORKSPACE_WITH_PIN(
        Pattern.compile("^@(?<name>\\w+)//file:(?<path>.+)\\.jar$"),
        (base, m) ->
            base.resolve(m.group("name")).resolve("file").resolve(m.group("path") + ".jar"));

    private final Pattern pattern;
    private final BiFunction<Path, Matcher, Path> parser;

    DependencyPatterns(Pattern pattern, BiFunction<Path, Matcher, Path> parser) {
      this.pattern = pattern;
      this.parser = parser;
    }

    Path tryMatch(Path outputBase, String raw) {
      var m = pattern.matcher(raw);
      if (m.matches()) {
        return parser.apply(outputBase, m);
      }

      return null;
    }

    static Path tryMatch(Path outputBase, boolean isModule, String raw) {
      if (isModule) {
        var dep = MODULE_WITH_PIN.tryMatch(outputBase, raw);
        if (dep != null) {
          return dep;
        }

        dep = MODULE_NO_PIN.tryMatch(outputBase, raw);
        return dep;
      }

      var dep = WORKSPACE_WITH_PIN.tryMatch(outputBase, raw);
      if (dep != null) {
        return dep;
      }

      dep = WORKSPACE_NO_PIN.tryMatch(outputBase, raw);
      return dep;
    }
  }
}
