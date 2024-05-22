package com.nikodoko.javaimports.environment.bazel;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.environment.jarutil.JarIdentifierLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

class BazelClassLoader {
  private static Logger log = Logs.getLogger(BazelClassLoader.class.getName());

  private final JarIdentifierLoader loader;

  BazelClassLoader(List<Path> deps) {
    this.loader = new JarIdentifierLoader(deps);
  }

  Optional<ClassEntity> findClass(Import i) {
    var span = Traces.createSpan("BazelClassLoader.findClass", new Tag("import", i));
    try (var __ = Traces.activate(span)) {
      var c = findClassInstrumented(i);
      Traces.addTags(span, new Tag("class", c.get()));
      return c;
    } catch (Throwable t) {
      log.log(Level.WARNING, String.format("Error finding class for import %s", i), t);

      Traces.addThrowable(span, t);
      return Optional.empty();
    } finally {
      span.finish();
    }
  }

  private Optional<ClassEntity> findClassInstrumented(Import i) {
    var c = ClassEntity.named(i.selector).declaring(loader.loadIdentifiers(i)).build();
    return Optional.of(c);
  }
}
