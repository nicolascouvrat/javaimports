package com.nikodoko.javaimports.environment.maven;

import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.environment.common.IdentifierLoader;
import com.nikodoko.javaimports.environment.jarutil.JarImportLoader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Loads a .jar, extracting all importable symbols. */
// TODO: handle static imports
class MavenDependencyLoader {
  static List<Import> load(Path dependency) throws IOException {
    var span =
        Traces.createSpan("MavenDependencyLoader.load", new Tag("dependency_path", dependency));
    try (var __ = Traces.activate(span)) {
      return loadInstrumented(dependency);
    } finally {
      span.finish();
    }
  }

  public static class Dependency {
    private final Set<Import> imports;
    private final Map<Import, Set<String>> identifiersByImport;
    private final IdentifierLoader loader;

    Dependency(Set<Import> imports, IdentifierLoader loader) {
      this.imports = imports;
      this.loader = loader;
      this.identifiersByImport = new HashMap<>();
    }

    public Set<Import> imports() {
      return imports;
    }

    public Set<String> findIdentifiers(Import i) {
      if (!imports.contains(i)) {
        return Set.of();
      }

      return identifiersByImport.computeIfAbsent(i, loader::loadIdentifiers);
    }
  }

  // static Dependency loadTEST(Path dependency) throws IOException {
  //   var imports = JarImportLoader.loadImports(dependency);
  //   return new Dependency(imports, new JarIdentifierLoader(dependency));
  // }

  // REAL API
  private static List<Import> loadInstrumented(Path dependency) throws IOException {
    return new ArrayList<>(JarImportLoader.loadImports(dependency));
  }
}
