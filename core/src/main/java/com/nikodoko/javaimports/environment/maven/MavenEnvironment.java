package com.nikodoko.javaimports.environment.maven;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.environment.Environment;
import com.nikodoko.javaimports.environment.jarutil.LazyJar;
import com.nikodoko.javaimports.environment.jarutil.LazyJars;
import com.nikodoko.javaimports.environment.shared.JavaProject;
import com.nikodoko.javaimports.environment.shared.ProjectParser;
import com.nikodoko.javaimports.parser.ParsedFile;
import io.opentracing.Span;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encapsulates a Maven project environment, scanning project files and dependencies for importable
 * symbols.
 */
public class MavenEnvironment implements Environment {
  private static Logger log = Logs.getLogger(MavenEnvironment.class.getName());
  private static final Path DEFAULT_REPOSITORY =
      Paths.get(System.getProperty("user.home"), ".m2/repository");
  private static final Clock clock = Clock.systemDefaultZone();

  private final Path root;
  private final Path fileBeingResolved;
  private final String pkgBeingResolved;
  private final Options options;
  private final MavenDependencyResolver resolver;
  private final MavenRepository repository;

  private MavenClassLoader classLoader;
  private Map<Identifier, List<Import>> availableImports = new HashMap<>();
  private JavaProject project;
  private boolean projectIsParsed = false;
  private boolean isInitialized = false;

  public MavenEnvironment(
      Path root, Path fileBeingResolved, String pkgBeingResolved, Options options) {
    this.root = root;
    this.fileBeingResolved = fileBeingResolved;
    this.pkgBeingResolved = pkgBeingResolved;
    this.options = options;
    var repository = options.repository();
    this.resolver = MavenDependencyResolver.withRepository(repository);
    this.repository = new LocalMavenRepository(resolver);
  }

  @Override
  public Set<ParsedFile> siblings() {
    parseProjectIfNeeded();
    return Sets.newHashSet(project.filesInPackage(pkgBeingResolved));
  }

  @Override
  public Collection<Import> findImports(Identifier i) {
    var span = Traces.createSpan("MavenEnvironment.findImports", new Tag("identifier", i));
    try (var __ = Traces.activate(span)) {
      return findImportsInstrumented(i);
    } finally {
      span.finish();
    }
  }

  private Collection<Import> findImportsInstrumented(Identifier i) {
    if (!isInitialized) {
      init();
    }

    var found = new ArrayList<Import>();
    found.addAll(availableImports.getOrDefault(i, List.of()));
    for (var file : project.allFiles()) {
      found.addAll(file.findImportables(i));
    }

    return found;
  }

  @Override
  public Optional<ClassEntity> findClass(Import i) {
    if (!isInitialized) {
      init();
    }

    // Try to find it in project first
    for (var file : project.allFiles()) {
      var maybeParent = file.findClass(i);
      if (maybeParent.isPresent()) {
        return maybeParent;
      }
    }

    // We do not want to try to look for the class if the environment does not provide this import
    // TODO: do not recompute the set each time
    var allImports =
        availableImports.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    if (!allImports.contains(i)) {
      return Optional.empty();
    }

    return classLoader.findClass(i);
  }

  private void init() {
    var span = Traces.createSpan("MavenEnvironment.init");
    try (var __ = Traces.activate(span)) {
      initInstrumented();
    } finally {
      span.finish();
    }
  }

  private void initInstrumented() {
    parseProjectIfNeeded();

    var start = clock.millis();
    var directDependencies = findDirectDependencies();
    var imports = extractImportsInDependencies(directDependencies);

    availableImports =
        imports.stream().collect(Collectors.groupingBy(i -> i.selector.identifier()));
    classLoader =
        new MavenClassLoader(
            repository, c -> resolver.resolve(c).jar, LazyJars::new, directDependencies);
    isInitialized = true;
    log.log(Level.INFO, String.format("init completed in %d ms", clock.millis() - start));
  }

  private void parseProjectIfNeeded() {
    if (projectIsParsed) {
      return;
    }
    long start = clock.millis();

    var srcs = MavenProjectFinder.withRoot(root).exclude(fileBeingResolved);
    var parser = new ProjectParser(srcs, options.executor());
    var parsed = parser.parseAll();
    log.info(
        String.format(
            "parsed project in %d ms (total of %d files)",
            clock.millis() - start, Iterables.size(parsed.project().allFiles())));

    parsed.errors().forEach(e -> log.log(Level.WARNING, "error parsing project", e));

    project = parsed.project();
    projectIsParsed = true;
  }

  private List<MavenDependency> findDirectDependencies() {
    var direct = new MavenDependencyFinder(repository).findAll(root);
    log.info(String.format("found %d direct dependencies: %s", direct.dependencies.size(), direct));

    return direct.dependencies;
  }

  private List<Import> extractImportsInDependencies(List<MavenDependency> directDependencies) {
    var loadedDirect = resolveAndLoad(directDependencies, new Tag("direct_dependencies", true));
    var emptyDirectDeps =
        loadedDirect.stream()
            .filter(d -> d.importables.isEmpty())
            .map(d -> d.dependency)
            .collect(Collectors.toList());
    // In order to handle cases like `org.junit.jupiter.junit-jupiter`, that point to an empty jar
    // with a point which in turns point to the actual API, we also load first-level transitive
    // dependencies for empty direct dependencies.
    // We do not bother with loading more transitive dependencies than that though, as it could
    // quickly become too slow for big projects. Since relying on transitive dependencies is not a
    // good practice anyway, we explicitely choose to not support it here.
    var indirectDependencies = repository.getTransitiveDependencies(emptyDirectDeps, 1);
    var loadedIndirect =
        resolveAndLoad(indirectDependencies, new Tag("direct_dependencies", false));
    log.info(
        String.format(
            "found %d indirect dependencies: %s",
            indirectDependencies.size(), indirectDependencies));
    return Stream.concat(loadedDirect.stream(), loadedIndirect.stream())
        .flatMap(d -> d.importables.stream())
        .collect(Collectors.toList());
  }

  private static class LoadedDependency {
    final List<Import> importables;
    final MavenDependency dependency;
    final List<MavenDependency> dependencies;

    LoadedDependency(
        List<Import> importables, List<MavenDependency> dependencies, MavenDependency dependency) {
      this.importables = importables;
      this.dependencies = dependencies;
      this.dependency = dependency;
    }
  }

  private List<LoadedDependency> resolveAndLoad(List<MavenDependency> dependencies, Tag tag) {
    var span =
        Traces.createSpan(
            "MavenEnvironment.resolveAndLoad",
            tag,
            new Tag("dependency_count", dependencies.size()));
    try (var __ = Traces.activate(span)) {
      return resolveAndLoadInstrumented(span, dependencies);
    } finally {
      span.finish();
    }
  }

  private List<LoadedDependency> resolveAndLoadInstrumented(
      Span span, List<MavenDependency> dependencies) {
    var futures =
        dependencies.stream()
            .map(
                d ->
                    CompletableFuture.supplyAsync(
                        () -> resolveAndLoad(span, d), options.executor()))
            .collect(Collectors.toList());

    CompletableFuture.allOf(futures.stream().toArray(CompletableFuture[]::new)).join();
    return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
  }

  private LoadedDependency resolveAndLoad(Span span, MavenDependency dependency) {
    var loaded = new LoadedDependency(List.of(), List.of(), dependency);
    var start = clock.millis();
    try (var __ = Traces.activate(span)) {
      var location = resolver.resolve(dependency);
      log.info(String.format("looking for dependency %s at %s", dependency, location));

      // var importables = MavenDependencyLoader.load(location.jar);
      var importables = new ArrayList<>(new LazyJar(location.jar).importables());
      var dependencies = MavenPomLoader.load(location.pom).pom.dependencies();
      loaded = new LoadedDependency(importables, dependencies, dependency);
    } catch (Exception e) {
      // No matter what happens, we don't want to fail the whole importing process just for that.
      log.log(Level.WARNING, String.format("could not resolve dependency %s", dependency), e);
    } finally {
      log.log(
          Level.INFO,
          String.format(
              "loaded %d imports and %d additional dependencies in %d ms (%s)",
              loaded.importables.size(),
              loaded.dependencies.size(),
              clock.millis() - start,
              dependency));
    }

    return loaded;
  }
}
