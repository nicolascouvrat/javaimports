package com.nikodoko.javaimports.environment.maven;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.environment.common.IdentifierLoader;
import com.nikodoko.javaimports.environment.jarutil.JarIdentifierLoader;
import com.nikodoko.javaimports.environment.jarutil.JarImportLoader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Loads a .jar, extracting all importable symbols. */
// TODO: handle static imports
class MavenDependencyLoader {
  static Dependency load(Path dependency) throws IOException {
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
    private final Map<Import, ClassEntity> classesByImport;
    private final IdentifierLoader loader;

    Dependency(Set<Import> imports, IdentifierLoader loader) {
      this.imports = imports;
      this.loader = loader;
      this.classesByImport = new HashMap<>();
    }

    public Set<Import> imports() {
      return imports;
    }

    public Optional<ClassEntity> findClass(Import i) {
      if (!imports.contains(i)) {
        return Optional.empty();
      }

      return Optional.of(classesByImport.computeIfAbsent(i, this::loadClass));
    }

    private ClassEntity loadClass(Import i) {
      return ClassEntity.named(i.selector).declaring(loader.loadIdentifiers(i)).build();
    }
  }

  // static Dependency loadTEST(Path dependency) throws IOException {
  //   var imports = JarImportLoader.loadImports(dependency);
  //   return new Dependency(imports, new JarIdentifierLoader(dependency));
  // }

  // REAL API
  private static Dependency loadInstrumented(Path dependency) throws IOException {
    var imports = JarImportLoader.loadImports(dependency);
    return new Dependency(imports, new JarIdentifierLoader(dependency));
  }
}
