package com.nikodoko.javaimports.environment.maven;

import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.environment.jarutil.IdentifierLoader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MavenClassLoader {
  // This is a little artificial, but it's easier to test than directly using
  // MavenDependencyResolver
  @FunctionalInterface
  static interface CoordinatesResolver {
    Path resolve(MavenCoordinates coordinates) throws IOException;
  }

  private static Logger log = Logger.getLogger(MavenEnvironment.class.getName());

  private final MavenRepository repository;
  private final CoordinatesResolver resolver;
  private final List<MavenDependency> directDependencies;
  private final IdentifierLoader.Factory loaderFactory;
  private final Options options;

  private IdentifierLoader loader = null;

  public MavenClassLoader(
      MavenRepository repository,
      CoordinatesResolver resolver,
      IdentifierLoader.Factory loaderFactory,
      List<MavenDependency> directDependencies,
      Options options) {
    this.repository = repository;
    this.resolver = resolver;
    this.loaderFactory = loaderFactory;
    this.directDependencies = directDependencies;
    this.options = options;
  }

  private void init() {
    var span = Traces.createSpan("MavenClassLoader.init");
    try (var __ = Traces.activate(span)) {
      initInstrumented();
    } catch (Throwable t) {
      if (options.debug()) {
        log.log(Level.WARNING, String.format("Error initializing MavenClassLoader: %s", t));
      }

      this.loader = i -> Set.of();
    } finally {
      span.finish();
    }
  }

  private void initInstrumented() {
    var transitiveDependencies = repository.getTransitiveDependencies(directDependencies, -1);
    var allDependencies =
        Stream.concat(transitiveDependencies.stream(), directDependencies.stream());
    var paths =
        allDependencies
            .map(this::maybeFindDependency)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    this.loader = loaderFactory.of(paths);
  }

  private Optional<Path> maybeFindDependency(MavenDependency d) {
    try {
      return Optional.of(resolver.resolve(d.coordinates()));
    } catch (Throwable t) {
      if (options.debug()) {
        log.log(Level.WARNING, String.format("Error resolving dependency %s: %s", d, t));
      }

      return Optional.empty();
    }
  }

  public Optional<ClassEntity> findClass(Import i) {
    var span = Traces.createSpan("MavenClassLoader.findClass", new Tag("import", i));
    try (var __ = Traces.activate(span)) {
      return findClassInstrumented(i);
    } catch (Throwable t) {
      if (options.debug()) {
        log.log(Level.WARNING, String.format("Error finding class for import %s: %s", i, t));
      }

      return Optional.empty();
    } finally {
      span.finish();
    }
  }

  private Optional<ClassEntity> findClassInstrumented(Import i) {
    if (loader == null) {
      init();
    }

    var c = ClassEntity.named(i.selector).declaring(loader.loadIdentifiers(i)).build();
    return Optional.of(c);
  }
}
