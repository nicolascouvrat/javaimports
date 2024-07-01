package com.nikodoko.javaimports.environment;

import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.JavaSourceFile;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.environment.bazel.BazelEnvironment;
import com.nikodoko.javaimports.environment.maven.MavenEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Environments {
  private static class DummyEnvironment implements Environment {
    @Override
    public Collection<Import> findImports(Identifier i) {
      return List.of();
    }

    @Override
    public Optional<ClassEntity> findClass(Import i) {
      return Optional.empty();
    }

    @Override
    public List<JavaSourceFile> siblings() {
      return List.of();
    }
  }

  public static Environment empty() {
    return new DummyEnvironment();
  }

  public static Environment autoSelect(Path filename, Selector pkg, Options options) {
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
        return initBazelEnvironment(current, filename, pkg, options);
      }

      current = current.getParent();
    }

    return new DummyEnvironment();
  }

  private static Environment initBazelEnvironment(
      Path targetRoot, Path filename, Selector pkg, Options options) {
    // Iterate further to find the workspace root | module root
    Path current = targetRoot;
    while (current != null) {
      Path potentialWorkspace = Paths.get(current.toString(), "WORKSPACE");
      Path potentialWorkspaceBazel = Paths.get(current.toString(), "WORKSPACE.bazel");
      Path potentialModule = Paths.get(current.toString(), "MODULE");
      Path potentialModuleBazel = Paths.get(current.toString(), "MODULE.bazel");
      if (Files.exists(potentialWorkspace) | Files.exists(potentialWorkspaceBazel)) {
        return new BazelEnvironment(current, targetRoot, false, filename, pkg, options);
      }

      if (Files.exists(potentialModule) | Files.exists(potentialModuleBazel)) {
        return new BazelEnvironment(current, targetRoot, true, filename, pkg, options);
      }

      current = current.getParent();
    }

    return new DummyEnvironment();
  }
}
