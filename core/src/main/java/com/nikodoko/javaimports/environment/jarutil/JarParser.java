package com.nikodoko.javaimports.environment.jarutil;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.environment.jarutil.classfile.BinaryNames;
import com.nikodoko.javaimports.environment.jarutil.classfile.Classfile;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarParser {
  private final Path path;

  public JarParser(Path path) {
    this.path = path;
  }

  public Optional<ClassEntity> parse(Import i) {
    try (var zip = new ZipFile(path.toFile())) {
      var entry = zip.getEntry(toPath(i));
      if (entry == null) {
        return Optional.empty();
      }

      try (var dis = open(zip, entry)) {
        return Optional.of(Classfile.readFrom(dis));
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("could not parse %s in jar %s".formatted(i, path), e);
    }
  }

  private static DataInputStream open(ZipFile file, ZipEntry entry) throws IOException {
    return new DataInputStream(new BufferedInputStream(file.getInputStream(entry), 8192));
  }

  private static final String CLASS_EXTENSION = ".class";

  private static String toPath(Import i) {
    var binaryName = BinaryNames.fromSelector(i.selector);
    return binaryName + CLASS_EXTENSION;
  }
}
