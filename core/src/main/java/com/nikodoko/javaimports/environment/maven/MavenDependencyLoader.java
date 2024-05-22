package com.nikodoko.javaimports.environment.maven;

import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/** Loads a .jar, extracting all importable symbols. */
// TODO: handle static imports
public class MavenDependencyLoader {
  private static final String SUBCLASS_SEPARATOR = "$";
  private static final String JAVA_9_MODULE_INFO = "module-info.class";
  private static final String DOT = ".";
  private static final String SEPARATOR = File.separator;
  private static final String CLASS_EXTENSION = ".class";

  public static List<Import> load(Path dependency) throws IOException {
    var span =
        Traces.createSpan("MavenDependencyLoader.load", new Tag("dependency_path", dependency));
    try (var __ = Traces.activate(span)) {
      return scanJar(dependency);
    } finally {
      span.finish();
    }
  }

  private static List<Import> scanJar(Path jar) throws IOException {
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
  private static boolean isValidImport(JarEntry entry) {
    return entry.getName().endsWith(CLASS_EXTENSION) && !entry.getName().equals(JAVA_9_MODULE_INFO);
  }
}
