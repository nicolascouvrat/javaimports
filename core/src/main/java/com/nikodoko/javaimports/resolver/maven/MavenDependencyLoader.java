package com.nikodoko.javaimports.resolver.maven;

import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.resolver.ImportWithDistance;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

class MavenDependencyLoader {
  private static final String SUBCLASS_SEPARATOR = "$";
  final Path fileBeingResolved;
  final MavenDependencyResolver resolver;

  MavenDependencyLoader(Path fileBeingResolved, Path repository) {
    this.fileBeingResolved = fileBeingResolved;
    this.resolver = MavenDependencyResolver.withRepository(repository);
  }

  List<ImportWithDistance> load(Path dependency) throws IOException {
    return scanJar(dependency);
  }

  private List<ImportWithDistance> scanJar(Path jar) throws IOException {
    List<ImportWithDistance> imports = new ArrayList<>();
    try (JarInputStream in = new JarInputStream(new FileInputStream(jar.toString()))) {
      JarEntry entry;
      while ((entry = in.getNextJarEntry()) != null) {
        // XXX: this will get all classes, including private and protected ones
        // TODO: properly handle module-class.class
        if (entry.getName().endsWith(".class")) {
          Import i = parseImport(Paths.get(entry.getName()));
          imports.add(ImportWithDistance.of(i, jar, fileBeingResolved));
        }
      }
    }

    return imports;
  }

  private Import parseImport(Path jarEntry) {
    String pkg = jarEntry.getParent().toString().replace("/", ".");
    String filename = jarEntry.getFileName().toString();
    String name = filename.substring(0, filename.lastIndexOf("."));
    if (!name.contains(SUBCLASS_SEPARATOR)) {
      return new Import(name, pkg, false);
    }

    // Make the subclass addressable by its name
    String extraPkg =
        name.substring(0, name.lastIndexOf(SUBCLASS_SEPARATOR)).replace(SUBCLASS_SEPARATOR, ".");
    String subclassName = name.substring(name.lastIndexOf(SUBCLASS_SEPARATOR) + 1, name.length());
    return new Import(subclassName, String.join(".", pkg, extraPkg), false);
  }

  public Path resolve(MavenDependency dependency) throws IOException {
    return resolver.resolve(dependency);
  }
}
