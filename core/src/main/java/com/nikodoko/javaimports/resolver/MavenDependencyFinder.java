package com.nikodoko.javaimports.resolver;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;

class MavenDependencyFinder {
  private List<MavenDependency> dependencies = new ArrayList<>();
  private boolean allFound = false;

  void scan(Reader reader) throws IOException {
    Model model = new DefaultModelReader().read(reader, null);
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

  boolean allFound() {
    return allFound;
  }

  List<MavenDependency> result() {
    return dependencies;
  }
}
