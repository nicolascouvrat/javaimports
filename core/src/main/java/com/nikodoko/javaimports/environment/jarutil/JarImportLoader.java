package com.nikodoko.javaimports.environment.jarutil;

import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/** Loads all imports for a given jar file. */
public class JarImportLoader {
  private static final String SUBCLASS_SEPARATOR = "$";
  private static final String JAVA_9_MODULE_INFO = "module-info.class";
  private static final String SEPARATOR = File.separator;
  private static final String CLASS_EXTENSION = ".class";

  public static Set<Import> loadImports(Path jarPath) throws IOException {
    Set<Import> imports = new HashSet<>();
    try (JarInputStream in = new JarInputStream(new FileInputStream(jarPath.toString()))) {
      JarEntry entry;
      while ((entry = in.getNextJarEntry()) != null) {
        // XXX: this will get all classes, including private and protected ones
        if (isUsableImport(entry)) {
          imports.add(parseImport(Paths.get(entry.getName())));
        }
      }
    }

    return imports;
  }

  private static Import parseImport(Path jarEntry) {
    return new Import(parseSelector(jarEntry), false);
  }

  private static Selector parseSelector(Path jarEntry) {
    var withoutExtension = jarEntry.toString().replace(CLASS_EXTENSION, "");
    withoutExtension = withoutExtension.replace(SUBCLASS_SEPARATOR, SEPARATOR);
    return Selector.of(Arrays.asList(withoutExtension.split(SEPARATOR)));
  }

  // TODO: we could be smarter and parse the module-info file to know what to import and what to
  // ignore.
  private static boolean isUsableImport(JarEntry entry) {
    return entry.getName().endsWith(CLASS_EXTENSION) && !entry.getName().equals(JAVA_9_MODULE_INFO);
  }
}
