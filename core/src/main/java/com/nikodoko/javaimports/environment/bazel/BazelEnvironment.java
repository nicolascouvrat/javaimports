package com.nikodoko.javaimports.environment.bazel;

import com.google.common.collect.Iterables;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.JavaSourceFile;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Utils;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.environment.Environment;
import com.nikodoko.javaimports.environment.maven.MavenDependencyLoader;
import com.nikodoko.javaimports.environment.shared.Dependency;
import com.nikodoko.javaimports.environment.shared.LazyJars;
import com.nikodoko.javaimports.environment.shared.LazyJavaProject;
import io.opentracing.Span;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BazelEnvironment implements Environment {
  private static final Clock clock = Clock.systemDefaultZone();
  private static Logger log = Logs.getLogger(BazelEnvironment.class.getName());
  private static final Path BAZEL_OUTPUT_USER_ROOT =
      Paths.get(System.getProperty("user.home"), ".javaimports");

  private final Path targetRoot;
  private final Path outputBase;
  private final Path workspaceRoot;
  private final Path fileBeingResolved;
  private final Selector pkgBeingResolved;
  private final Options options;
  private final boolean isModule;

  // TODO: this should be set after we do the initial bazel query
  private Precision precision = Precision.MINIMAL;

  private BazelQueryResults cache = null;
  private LazyJavaProject project = null;
  private Map<Identifier, List<Import>> availableImports = null;
  private LazyJars jars = null;

  // TMP
  private static final Path BAZEL_REPOSITORY_CACHE =
      Paths.get(System.getProperty("user.home"), ".javaimports_tmp", "bazel", "repository");
  private static final Path BAZEL_LOCAL_CACHE =
      Paths.get(System.getProperty("user.home"), ".javaimports_tmp", "bazel", "disk");

  static {
    try {
      Files.createDirectories(BAZEL_REPOSITORY_CACHE);
      Files.createDirectories(BAZEL_LOCAL_CACHE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public BazelEnvironment(
      Path workspaceRoot,
      Path targetRoot,
      boolean isModule,
      Path fileBeingResolved,
      Selector pkgBeingResolved,
      Options options) {
    this.outputBase = outputBase(workspaceRoot);
    this.workspaceRoot = workspaceRoot;
    this.targetRoot = targetRoot;
    this.isModule = isModule;
    this.fileBeingResolved = fileBeingResolved;
    this.options = options;
    this.pkgBeingResolved = pkgBeingResolved;
  }

  private Path outputBase(Path workspaceRoot) {
    var md5Hash = Utils.md5(workspaceRoot.toString());
    var outputBase = BAZEL_OUTPUT_USER_ROOT.resolve(md5Hash);
    try {
      Files.createDirectories(outputBase);
    } catch (IOException e) {
      log.log(
          Level.WARNING,
          "could not create output_base %s, using default instead".formatted(outputBase),
          e);
      return null;
    }
    return outputBase;
  }

  private LazyJars jars() {
    if (jars == null) {
      var span = Traces.createSpan("BazelEnvironment.initJars");
      try (var __ = Traces.activate(span)) {
        jars = initJars();
      } finally {
        span.finish();
      }
    }

    return jars;
  }

  private LazyJars initJars() {
    long start = clock.millis();
    return new LazyJars(options.executor(), cache().deps());
  }

  private LazyJavaProject project() {
    if (project == null) {
      var span = Traces.createSpan("BazelEnvironment.initProject");
      try (var __ = Traces.activate(span)) {
        project = initProject();
      } finally {
        span.finish();
      }
    }

    return project;
  }

  private LazyJavaProject initProject() {
    long start = clock.millis();
    var project = new LazyJavaProject(pkgBeingResolved, cache().srcs());
    log.info(
        String.format(
            "parsed project in %d ms (total of %d files)",
            clock.millis() - start, Iterables.size(project.allFiles())));

    return project;
  }

  private BazelQueryResults cache() {
    if (cache == null) {
      var span = Traces.createSpan("BazelEnvironment.initCache");
      try (var __ = Traces.activate(span)) {
        cache = initCache();
      } finally {
        span.finish();
      }
    }

    return cache;
  }

  private BazelQueryResults initCache() {
    var start = clock.millis();
    try {
      return bazelQuery();
    } catch (Exception e) {
      log.log(Level.WARNING, "init error", e);
      return new BazelQueryResults(List.of(), List.of());
    } finally {
      log.log(Level.INFO, String.format("init completed in %d ms", clock.millis() - start));
    }
  }

  private Map<Identifier, List<Import>> availableImports() {
    if (availableImports == null) {
      var span = Traces.createSpan("BazelEnvironment.initAvailableImports");
      try (var __ = Traces.activate(span)) {
        availableImports = initAvailableImports(span);
      } finally {
        span.finish();
      }
    }

    return availableImports;
  }

  private Map<Identifier, List<Import>> initAvailableImports(Span span) {
    var start = clock.millis();
    var tasks =
        cache().deps().stream()
            .map(BazelDependency::path)
            .map(d -> CompletableFuture.supplyAsync(() -> loadImports(span, d), options.executor()))
            .toList();
    return Utils.sequence(tasks)
        .thenApply(
            results ->
                results.stream()
                    .flatMap(List::stream)
                    .collect(Collectors.groupingBy(i -> i.selector.identifier())))
        .join();
  }

  private List<Import> loadImports(Span span, Path dep) {
    try (var __ = Traces.activate(span)) {
      return MavenDependencyLoader.load(dep);
    } catch (Exception e) {
      // throw new RuntimeException(e);
      log.log(Level.WARNING, String.format("could not load dependency %s", dep), e);
      return List.of();
    }
  }

  private static final String DEPS_FORMAT = "deps(attr('srcs', //%s:%s, //%s:*))";

  private BazelQueryResults bazelQuery() throws InterruptedException, IOException {
    var pkgPath = workspaceRoot.relativize(targetRoot);
    var filePath = targetRoot.relativize(fileBeingResolved);
    var deps = String.format(DEPS_FORMAT, pkgPath, filePath, pkgPath);
    log.log(
        Level.INFO,
        "running bazel query in for %s in %s (output_base %s)"
            .formatted(deps, workspaceRoot, outputBase));
    // As per Process' documentation: "Because some native platforms only provide limited buffer
    // size for standard input and output streams, failure to promptly write the input stream or
    // read the output stream of the process may cause the process to block, or even deadlock."
    //
    // We therefore need to redirect output, and do that in parallel so that we can handle both
    // stdout and stderr accordingly.
    var stderrRedirect =
        options.debug() ? ProcessBuilder.Redirect.PIPE : ProcessBuilder.Redirect.DISCARD;
    var proc =
        new ProcessBuilder(
                "bazel",
                "--output_base=%s".formatted(outputBase),
                "query",
                "--disk_cache=%s".formatted(BAZEL_LOCAL_CACHE),
                "--repository_cache=%s".formatted(BAZEL_REPOSITORY_CACHE),
                "--output=minrank",
                deps)
            .redirectError(stderrRedirect)
            .directory(workspaceRoot.toFile())
            .start();

    options
        .executor()
        .execute(
            () -> {
              var reader = proc.errorReader();
              String line;
              try {
                while ((line = reader.readLine()) != null) {
                  log.log(Level.INFO, "(bazel query): " + line);
                }
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
    var results =
        BazelQueryResults.parser()
            .workspaceRoot(workspaceRoot)
            .outputBase(outputBase)
            .isModule(isModule)
            .parse(proc.inputReader());
    var exitCode = proc.waitFor();
    if (exitCode != 0) {
      log.log(Level.WARNING, "bazel query error (code %d)".formatted(exitCode));
    }

    return results;
  }

  @Override
  public List<? extends JavaSourceFile> siblings() {
    return project().filesInPackage(pkgBeingResolved);
  }

  enum Precision {
    // We have:
    // - Imports:
    //    - In the project, they are derived from file names of direct dependencies exclusively
    //    - In the JARs, we have no imports
    // - Classes:
    //    - In the project, we will parse a file on demand if that file name matches the requested
    //    import
    //    - In the JARs, we will load the jar on demand if its path contains one or multiple words
    //    in the request import's selector. Then, if it turns out it indeed contains the requested
    //    import, we will parse the matching class
    MINIMAL,
    // In this step, we load all JARs that are direct dependencies, meaning that their imports will
    // become available
    ALL_DIRECT_JARS,
    // In this step, we eagerly parse all files from direct dependencies, which will expose all
    // static identifiers/classes in the import list, as well as the matching classes if any
    ALL_DIRECT_DEPS,
    // In this step, we load all JARs that are transitive dependencies, meaning their imports will
    // become available. We also make all transitive source files available in a lazy way (i.e.
    // based on name only)
    ALL_JARS,
    // Finally, we eagerly parse all files from transitive dependencies.
    MAXIMAL;
  }

  @Override
  public boolean increasePrecision() {
    if (precision == Precision.MINIMAL) {
      jars().load(Dependency.Kind.DIRECT);
      precision = Precision.ALL_DIRECT_JARS;
      return true;
    }

    if (precision == Precision.ALL_DIRECT_JARS) {
      project().eagerlyParse(options.executor());
      precision = Precision.ALL_DIRECT_DEPS;
      return true;
    }

    if (precision == Precision.ALL_DIRECT_DEPS) {
      jars().load(Dependency.Kind.TRANSITIVE);
      project().includeTransitive();
      precision = Precision.ALL_JARS;
      return true;
    }

    if (precision == Precision.ALL_JARS) {
      project().eagerlyParse(options.executor());
      precision = Precision.MAXIMAL;
      return true;
    }

    return false;
  }

  @Override
  public Collection<Import> findImports(Identifier i) {
    var found = new ArrayList<Import>();
    found.addAll(jars().findImports(i));
    for (var file : project().allFiles()) {
      found.addAll(file.findImports(i));
    }

    return found;
  }

  @Override
  public Optional<ClassEntity> findClass(Import i) {
    for (var file : project().allFiles()) {
      var maybeParent = file.findClass(i);
      if (maybeParent.isPresent()) {
        return maybeParent;
      }
    }

    // // We do not want to try to look for the class if the environment does not provide this
    // import
    // // TODO: do not recompute the set each time
    // var allImports =
    //     availableImports.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    // if (!allImports.contains(i)) {
    //   return Optional.empty();
    // }

    return jars().findClass(i);
  }
}
