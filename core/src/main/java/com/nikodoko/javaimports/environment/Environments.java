package com.nikodoko.javaimports.environment;

import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.environment.bazel.BazelEnvironment;
import com.nikodoko.javaimports.environment.maven.MavenEnvironment;
import com.nikodoko.javaimports.parser.ParsedFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Environments {
  private static class DummyEnvironment implements Environment {
    @Override
    public Set<ParsedFile> filesInPackage(String packageName) {
      return new HashSet<>();
    }

    @Override
    public Collection<Import> findImports(Identifier i) {
      return List.of();
    }

    @Override
    public Optional<ClassEntity> findClass(Import i) {
      return Optional.empty();
    }
  }

  public static Environment empty() {
    return new DummyEnvironment();
  }

  public static Environment autoSelect(Path filename, String pkg, Options options) {
    Path current = filename.getParent();
    while (current != null) {
      // Prioritize POM
      Path potentialPom = Paths.get(current.toString(), "pom.xml");
      if (Files.exists(potentialPom)) {
        return new MavenEnvironment(current, filename, pkg, options);
      }

      Path potentialBuild = Paths.get(current.toString(), "BUILD");
      Path potentialBuildBazel = Paths.get(current.toString(), "BUILD.bazel");
      if (Files.exists(potentialBuild) || Files.exists(potentialBuildBazel)) {
        return initBazelEnvironment(current, filename, options);
      }

      current = current.getParent();
    }

    return new DummyEnvironment();
  }

  private static Environment initBazelEnvironment(Path targetRoot, Path filename, Options options) {
    // Iterate further to find the workspace root | module root
    Path current = targetRoot;
    while (current != null) {
      Path potentialWorkspace = Paths.get(current.toString(), "WORKSPACE");
      Path potentialWorkspaceBazel = Paths.get(current.toString(), "WORKSPACE.bazel");
      Path potentialModule = Paths.get(current.toString(), "MODULE");
      Path potentialModuleBazel = Paths.get(current.toString(), "MODULE.bazel");
      if (Files.exists(potentialWorkspace)
          | Files.exists(potentialWorkspaceBazel)
          | Files.exists(potentialModule)
          | Files.exists(potentialModuleBazel)) {
        return new BazelEnvironment(current, targetRoot, filename, options);
      }

      current = current.getParent();
    }

    return new DummyEnvironment();
  }
}
