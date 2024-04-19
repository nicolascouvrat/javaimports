package com.nikodoko.javaimports.environment.maven;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.environment.jarutil.IdentifierLoader;
import io.opentracing.Span;
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

  private static Logger log = Logs.getLogger(MavenEnvironment.class.getName());

  private final MavenRepository repository;
  private final CoordinatesResolver resolver;
  private final List<MavenDependency> directDependencies;
  private final IdentifierLoader.Factory loaderFactory;

  private IdentifierLoader loader = null;

  public MavenClassLoader(
      MavenRepository repository,
      CoordinatesResolver resolver,
      IdentifierLoader.Factory loaderFactory,
      List<MavenDependency> directDependencies) {
    this.repository = repository;
    this.resolver = resolver;
    this.loaderFactory = loaderFactory;
    this.directDependencies = directDependencies;
  }

  private void init() {
    var span = Traces.createSpan("MavenClassLoader.init");
    try (var __ = Traces.activate(span)) {
      initInstrumented(span);
    } catch (Throwable t) {
      log.log(Level.WARNING, "Error initializing MavenClassLoader", t);

      this.loader = i -> Set.of();
    } finally {
      span.finish();
    }
  }

  private void initInstrumented(Span span) {
    var transitiveDependencies = repository.getTransitiveDependencies(directDependencies, -1);
    var allDependencies =
        Stream.concat(transitiveDependencies.stream(), directDependencies.stream());
    var paths =
        allDependencies
            .map(this::maybeFindDependency)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    Traces.addTags(
        span,
        new Tag("transitive_deps_count", transitiveDependencies.size()),
        new Tag("paths_count", paths.size()));

    this.loader = loaderFactory.of(paths);
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

    var c = ClassEntity.named(i.selector).declaring(loader.loadIdentifiers(i)).build();
    return Optional.of(c);
  }
}
