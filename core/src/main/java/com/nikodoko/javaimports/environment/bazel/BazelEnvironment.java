package com.nikodoko.javaimports.environment.bazel;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Utils;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.environment.Environment;
import com.nikodoko.javaimports.environment.shared.JavaProject;
import com.nikodoko.javaimports.environment.shared.ProjectParser;
import com.nikodoko.javaimports.parser.ParsedFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BazelEnvironment implements Environment {
  private static final Clock clock = Clock.systemDefaultZone();
  private static Logger log = Logs.getLogger(BazelEnvironment.class.getName());
  private static final Path BAZEL_OUTPUT_USER_ROOT =
      Paths.get(System.getProperty("user.home"), ".javaimports");

  private final Path targetRoot;
  private final Path outputBase;
  private final Path fileBeingResolved;
  private final Options options;

  private BazelQueryResults cache = null;
  private JavaProject project = null;

  public BazelEnvironment(
      Path workspaceRoot, Path targetRoot, Path fileBeingResolved, Options options) {
    this.outputBase = outputBase(workspaceRoot);
    this.targetRoot = targetRoot;
    this.fileBeingResolved = fileBeingResolved;
    this.options = options;
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

  private JavaProject project() {
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

  private JavaProject initProject() {
    long start = clock.millis();
    var parser = new ProjectParser(cache(), options.executor());
    var parsed = parser.parseAll();
    log.info(
        String.format(
            "parsed project in %d ms (total of %d files)",
            clock.millis() - start, Iterables.size(parsed.project().allFiles())));

    parsed.errors().forEach(e -> log.log(Level.WARNING, "error parsing project", e));

    return parsed.project();
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

  private BazelQueryResults bazelQuery() throws InterruptedException, IOException {
    log.log(
        Level.INFO, "running bazel query in %s (output_base %s)".formatted(targetRoot, outputBase));
    // As per Process' documentation: "Because some native platforms only provide limited buffer
    // size for standard input and output streams, failure to promptly write the input stream or
    // read the output stream of the process may cause the process to block, or even deadlock."
    //
    // We therefore need to redirect output, and do that in parallel so that we can handle both
    // stdout and stderr accordingly.
    var stderrRedirect =
        options.debug() ? ProcessBuilder.Redirect.PIPE : ProcessBuilder.Redirect.DISCARD;
    var proc =
        new ProcessBuilder("bazel", "--output_base=%s".formatted(outputBase), "query", "deps(...)")
            .redirectError(stderrRedirect)
            .directory(targetRoot.toFile())
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
    var results = BazelQueryResults.parse(targetRoot, outputBase, proc.inputReader());
    var exitCode = proc.waitFor();
    if (exitCode != 0) {
      log.log(Level.WARNING, "bazel query error (code %d)".formatted(exitCode));
    }

    return results;
  }

  @Override
  public Set<ParsedFile> filesInPackage(String packageName) {
    return Sets.newHashSet(project().filesInPackage(packageName));
  }

  @Override
  public Collection<Import> findImports(Identifier i) {
    var found = new ArrayList<Import>();
    for (var file : project().allFiles()) {
      found.addAll(file.findImportables(i));
    }

    return found;
  }

  @Override
  public Optional<ClassEntity> findClass(Import i) {
    return Optional.empty();
  }
}
