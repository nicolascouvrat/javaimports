package com.nikodoko.javaimports.environment.jarutil;

import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.environment.jarutil.classfile.Classfile;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.StringJoiner;
import java.util.zip.ZipFile;

public class JarParser {
  private final Path path;

  public JarParser(Path path) {
    this.path = path;
  }

  public void parse(Import i) {
    try (var zip = new ZipFile(path.toFile())) {
      var entry = zip.getEntry(toPath(i));
      if (entry == null) {
        return;
      }

      try (var dis =
          new DataInputStream(new BufferedInputStream(zip.getInputStream(entry), 8192))) {
        var p = Classfile.readFrom(dis);
        System.out.println(p);
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("could not parse %s in jar %s".formatted(i, path), e);
    }
  }

  private static final String SUBCLASS_SEPARATOR = "$";
  private static final String JAVA_9_MODULE_INFO = "module-info.class";
  private static final String DOT = ".";
  private static final String SEPARATOR = File.separator;
  private static final String CLASS_EXTENSION = ".class";

  private static String toPath(Import i) {
    var s = i.isStatic ? i.selector.scope() : i.selector;
    var reversed = new ArrayList<Identifier>(s.size());
    do {
      reversed.add(s.identifier());
      s = s.scope();
    } while (s.size() > 1);

    var sj = new StringJoiner(SEPARATOR);
    sj.add(s.identifier().toString());
    for (var j = reversed.size() - 1; j >= 0; j--) {
      sj.add(reversed.get(j).toString());
    }

    if (i.isStatic) {
      return sj.toString() + SUBCLASS_SEPARATOR + i.selector.identifier() + CLASS_EXTENSION;
    }

    return sj.toString() + CLASS_EXTENSION;
  }
}
