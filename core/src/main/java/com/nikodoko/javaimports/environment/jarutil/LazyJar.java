package com.nikodoko.javaimports.environment.jarutil;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.JavaJar;
import com.nikodoko.javaimports.common.Utils;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.environment.jarutil.classfile.BinaryNames;
import com.nikodoko.javaimports.environment.jarutil.classfile.Classfile;
import io.opentracing.Span;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LazyJar implements JavaJar {
  private static final Tag.Key<Path> JAR_PATH = Tag.withKey("jar_path");
  private static final Logger log = Logs.getLogger(LazyJar.class.getName());
  private static final String CLASS_EXTENSION = ".class";

  private final Path path;
  private final Map<Import, Optional<ClassEntity>> classes = new ConcurrentHashMap<>();
  private volatile Set<Import> importables = null;

  public LazyJar(Path path) {
    this.path = path;
  }

  public Path path() {
    return path;
  }

  @Override
  public String toString() {
    return Utils.toStringHelper(this).add("path", path).toString();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof LazyJar)) {
      return false;
    }

    var that = (LazyJar) o;
    return Objects.equals(this.path, that.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path);
  }

  @Override
  public Collection<Import> findAllImports() {
    return importables();
  }

  @Override
  public Collection<Import> findImports(Identifier i) {
    return importables().stream()
        .filter(imprt -> imprt.selector.identifier().equals(i))
        .collect(Collectors.toSet());
  }

  private Set<Import> importables() {
    if (importables == null) {
      var span = Traces.createSpan("LazyJar.initImportables", JAR_PATH.is(path));
      try (var __ = Traces.activate(span)) {
        initImportables(span);
      } finally {
        span.finish();
      }
    }

    return importables;
  }

  private synchronized void initImportables(Span span) {
    if (importables != null) return;

    try (var zip = new ZipFile(path.toFile())) {
      importables =
          zip.stream()
              .filter(e -> JarEntryNames.isImportable(e.getName()))
              .map(e -> JarEntryNames.toImport(e.getName()))
              .collect(Collectors.toSet());
    } catch (Exception e) {
      log.log(Level.WARNING, "could not load importables for " + path, e);
      Traces.addThrowable(span, e);
      importables = Set.of();
    }
  }

  private static String toPath(Import i) {
    var binaryName = BinaryNames.fromSelector(i.selector);
    return binaryName + CLASS_EXTENSION;
  }

  @Override
  public Optional<ClassEntity> findClass(Import i) {
    if (!importables().contains(i)) {
      return Optional.empty();
    }

    return classes.computeIfAbsent(i, this::loadClass);
  }

  private Optional<ClassEntity> loadClass(Import i) {
    try (var zip = new ZipFile(path.toFile())) {
      var entry = zip.getEntry(toPath(i));
      try (var dis = open(zip, entry)) {
        return Optional.of(Classfile.readFrom(dis));
      }
    } catch (Exception e) {
      log.log(Level.WARNING, "could not load class " + i, e);
      return Optional.empty();
    }
  }

  private static DataInputStream open(ZipFile file, ZipEntry entry) throws IOException {
    return new DataInputStream(new BufferedInputStream(file.getInputStream(entry), 8192));
  }
}
