package com.nikodoko.javaimports.resolver.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;

class MavenDependencyFinder {
  private List<MavenDependency> dependencies = new ArrayList<>();
  private boolean allFound = false;

  List<MavenDependency> findAll(Path moduleRoot) throws IOException {
    Path target = moduleRoot;
    while (!allFound && hasPom(target)) {
      scanPom(target);
      target = target.getParent();
    }

    return dependencies;
  }

  private boolean hasPom(Path directory) {
    Path pom = Paths.get(directory.toString(), "pom.xml");
    return Files.exists(pom);
  }

  private void scanPom(Path directory) throws IOException {
    File pom = Paths.get(directory.toString(), "pom.xml").toFile();
    Model model = new DefaultModelReader().read(pom, null);
    List<Dependency> deps = model.getDependencies();
    scanDependencies(deps);
  }

  private void scanDependencies(List<Dependency> deps) {
    boolean ok = true;
    for (Dependency dep : deps) {
      MavenDependency dependency =
          new MavenDependency(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
      dependencies.add(dependency);

      if (!dependency.hasPlainVersion()) {
        ok = false;
      }
    }

    allFound = ok;
  }
}
