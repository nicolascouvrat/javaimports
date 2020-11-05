package com.nikodoko.javaimports.environment.maven;

import com.nikodoko.javaimports.parser.Import;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/** Loads a .jar, extracting all importable symbols. */
// TODO: handle static imports
class MavenDependencyLoader {
  private static final String SUBCLASS_SEPARATOR = "$";
  private static final String JAVA_9_MODULE_INFO = "module-info.class";

  List<Import> load(Path dependency) throws IOException {
    return scanJar(dependency);
  }

  private List<Import> scanJar(Path jar) throws IOException {
    List<Import> imports = new ArrayList<>();
    try (JarInputStream in = new JarInputStream(new FileInputStream(jar.toString()))) {
      JarEntry entry;
      while ((entry = in.getNextJarEntry()) != null) {
        // XXX: this will get all classes, including private and protected ones
        if (isValidImport(entry)) {
          imports.add(parseImport(Paths.get(entry.getName())));
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

  // TODO: we could be smarter and parse the module-info file to know what to import and what to
  // ignore.
  private boolean isValidImport(JarEntry entry) {
    return entry.getName().endsWith(".class") && !entry.getName().equals(JAVA_9_MODULE_INFO);
  }
}
