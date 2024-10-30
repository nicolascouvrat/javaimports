package com.nikodoko.javaimports.environment.maven;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.environment.shared.Dependency;
import com.nikodoko.javaimports.environment.shared.LazyJars;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class MavenClassLoader {
  // This is a little artificial, but it's easier to test than directly using
  // MavenDependencyResolver
  @FunctionalInterface
  static interface CoordinatesResolver {
    Path resolve(MavenCoordinates coordinates) throws IOException;
  }

  private static Logger log = Logs.getLogger(MavenEnvironment.class.getName());

  private final MavenRepository repository;
  private final CoordinatesResolver resolver;
  private final List<MavenDependency> directDependencies;
  private final Executor executor;

  private Optional<LazyJars> loader = null;

  public MavenClassLoader(
      MavenRepository repository,
      CoordinatesResolver resolver,
      Executor executor,
      List<MavenDependency> directDependencies) {
    this.repository = repository;
    this.resolver = resolver;
    this.directDependencies = directDependencies;
    this.executor = executor;
  }

  private void init() {
    var span = Traces.createSpan("MavenClassLoader.init");
    try (var __ = Traces.activate(span)) {
      initInstrumented();
    } catch (Throwable t) {
      log.log(Level.WARNING, "Error initializing MavenClassLoader", t);
      this.loader = Optional.empty();
    } finally {
      span.finish();
    }
  }

  private record MavenJar(Dependency.Kind kind, Path path) implements Dependency {}

  private void initInstrumented() {
    var transitive =
        repository.getTransitiveDependencies(directDependencies, -1).stream()
            .map(this::maybeFindDependency)
            .filter(Optional::isPresent)
            .map(p -> new MavenJar(Dependency.Kind.TRANSITIVE, p.get()));
    var direct =
        directDependencies.stream()
            .map(this::maybeFindDependency)
            .filter(Optional::isPresent)
            .map(p -> new MavenJar(Dependency.Kind.DIRECT, p.get()));
    var all = Stream.concat(transitive, direct).toList();
    var loader = new LazyJars(executor, all);
    // TODO: handle progressive load for maven
    loader.load(Dependency.Kind.TRANSITIVE);
    loader.load(Dependency.Kind.DIRECT);
    this.loader = Optional.of(loader);
  }

  private Optional<Path> maybeFindDependency(MavenDependency d) {
    try {
      return Optional.of(resolver.resolve(d.coordinates()));
    } catch (Throwable t) {
      log.log(Level.WARNING, String.format("Error resolving dependency %s: %s", d, t));

      return Optional.empty();
    }
  }

  public Optional<ClassEntity> findClass(Import i) {
    var span = Traces.createSpan("MavenClassLoader.findClass", new Tag("import", i));
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
    if (loader == null) {
      init();
    }

    return loader.map(l -> l.findClass(i)).orElse(Optional.empty());
  }
}
