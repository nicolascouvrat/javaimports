package com.nikodoko.javaimports.environment.shared;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.ClassProvider;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.ImportProvider;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Utils;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.common.telemetry.Traces;
import io.opentracing.Span;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class LazyJars implements ImportProvider, ClassProvider {
  private static Logger log = Logs.getLogger(LazyJars.class.getName());

  private final Executor executor;
  private final Map<Dependency.Kind, List<LazyJar>> depsByKind;
  private final Map<Set<String>, LazyJar> depsByPath;

  // Cache
  private final Map<Identifier, Set<Import>> availableImports = new HashMap<>();
  private final Set<LazyJar> loaded = new HashSet<>();

  public LazyJars(Executor executor, Collection<? extends Dependency> deps) {
    this.executor = executor;
    this.depsByKind =
        deps.stream()
            .collect(
                Collectors.groupingBy(
                    Dependency::kind,
                    Collectors.mapping(d -> new LazyJar(d.path()), Collectors.toList())));
    this.depsByPath =
        depsByKind.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toMap(jar -> split(jar.path()), Function.identity()));
  }

  private static Set<String> split(Path path) {
    return StreamSupport.stream(path.spliterator(), false)
        .map(Path::toString)
        .collect(Collectors.toSet());
  }

  public void load(Dependency.Kind kind) {
    load(depsByKind.getOrDefault(kind, List.of()));
  }

  @Override
  public Collection<Import> findImports(Identifier i) {
    return availableImports.getOrDefault(i, Set.of());
  }

  @Override
  public Optional<ClassEntity> findClass(Import i) {
    for (var jar : loaded) {
      var maybeClass = jar.findClass(i);
      if (maybeClass.isPresent()) {
        return maybeClass;
      }
    }

    // If we havent found anything, heuristically try to find the jar that _may_ contain this import
    var scoringFunction = similarityScore(i.selector);
    var candidates = new HashMap<Integer, List<LazyJar>>();
    for (var e : depsByPath.entrySet()) {
      var score = scoringFunction.apply(e.getKey());
      if (score == 0) {
        continue;
      }

      candidates.computeIfAbsent(score, __ -> new ArrayList<>()).add(e.getValue());
    }

    var scores = candidates.keySet().stream().sorted(Comparator.reverseOrder()).toList();
    for (var score : scores) {
      load(candidates.get(score));
      for (var jar : loaded) {
        var maybeClass = jar.findClass(i);
        if (maybeClass.isPresent()) {
          return maybeClass;
        }
      }
    }

    return Optional.empty();
  }

  private Function<Set<String>, Integer> similarityScore(Selector s) {
    var words = s.identifiers().stream().map(Identifier::toString).collect(Collectors.toSet());
    return elts -> {
      var score = 0;
      for (var w : words) {
        if (elts.contains(w)) score += 1;
      }
      return score;
    };
  }

  private void load(Collection<LazyJar> jars) {
    var toLoad = jars.stream().filter(jar -> !loaded.contains(jar)).toList();
    if (toLoad.isEmpty()) {
      return;
    }

    var span = Traces.createSpan("LazyJars.load");
    try (var __ = Traces.activate(span)) {
      load(toLoad, span);
    } finally {
      span.finish();
    }
  }

  private synchronized void load(Collection<LazyJar> jars, Span span) {
    var toLoad = jars.stream().filter(jar -> !loaded.contains(jar)).collect(Collectors.toSet());
    if (toLoad.isEmpty()) {
      return;
    }

    var tasks =
        toLoad.stream()
            .map(jar -> CompletableFuture.supplyAsync(() -> load(jar, span), executor))
            .toList();

    Utils.sequence(tasks)
        .thenAccept(
            results -> {
              for (var result : results) {
                for (var importable : result) {
                  availableImports
                      .computeIfAbsent(importable.selector.identifier(), __ -> new HashSet<>())
                      .add(importable);
                }
              }
            })
        .join();
    loaded.addAll(toLoad);
  }

  private Collection<Import> load(LazyJar jar, Span span) {
    try (var __ = Traces.activate(span)) {
      return jar.findAllImports();
    }
  }
}
