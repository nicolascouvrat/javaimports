package com.nikodoko.javaimports.environment.shared;

import static com.google.common.truth.Truth.assertThat;
import static com.nikodoko.javaimports.common.CommonTestUtil.aSelector;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

public class LazyJavaProjectTest {
  private static class DummySourceFile implements LazyParsedFile {
    private final Selector pkg;
    private boolean isParsed = false;

    private DummySourceFile(Selector pkg) {
      this.pkg = pkg;
    }

    @Override
    public CompletableFuture<Void> parseAsync(Executor e) {
      this.isParsed = true;
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public Selector pkg() {
      return pkg;
    }

    @Override
    public Set<Identifier> topLevelDeclarations() {
      return Set.of();
    }

    @Override
    public Collection<Import> findImports(Identifier i) {
      return List.of();
    }

    @Override
    public Optional<ClassEntity> findClass(Import i) {
      return Optional.empty();
    }
  }

  @Test
  void itShouldExposeOnlyDirectAtFirst() {
    var directAbc = new DummySourceFile(aSelector("a.b.c"));
    var transitiveAbc = new DummySourceFile(aSelector("a.b.c"));
    var files =
        Map.of(
            Dependency.Kind.DIRECT,
            List.of((LazyParsedFile) directAbc),
            Dependency.Kind.TRANSITIVE,
            List.of((LazyParsedFile) transitiveAbc));

    var project = new LazyJavaProject(files);

    assertThat(project.filesInPackage(aSelector("a.b.c"))).containsExactly(directAbc);
    assertThat(project.allFiles()).containsExactly(directAbc);
    assertThat(directAbc.isParsed).isFalse();
    assertThat(transitiveAbc.isParsed).isFalse();
  }

  @Test
  void itShouldExposeTransitiveWhenRequested() {
    var directAbc = new DummySourceFile(aSelector("a.b.c"));
    var transitiveAbc = new DummySourceFile(aSelector("a.b.c"));
    var files =
        Map.of(
            Dependency.Kind.DIRECT,
            List.of((LazyParsedFile) directAbc),
            Dependency.Kind.TRANSITIVE,
            List.of((LazyParsedFile) transitiveAbc));

    var project = new LazyJavaProject(files);
    project.includeTransitive();

    assertThat(project.filesInPackage(aSelector("a.b.c")))
        .containsExactly(directAbc, transitiveAbc);
    assertThat(project.allFiles()).containsExactly(directAbc, transitiveAbc);
    assertThat(directAbc.isParsed).isFalse();
    assertThat(transitiveAbc.isParsed).isFalse();
  }

  @Test
  void itShouldOnlyEagerlyParseExposedFiles() {
    var directAbc = new DummySourceFile(aSelector("a.b.c"));
    var transitiveAbc = new DummySourceFile(aSelector("a.b.c"));
    var files =
        Map.of(
            Dependency.Kind.DIRECT,
            List.of((LazyParsedFile) directAbc),
            Dependency.Kind.TRANSITIVE,
            List.of((LazyParsedFile) transitiveAbc));

    var project = new LazyJavaProject(files);
    project.eagerlyParse(runnable -> {});

    assertThat(directAbc.isParsed).isTrue();
    assertThat(transitiveAbc.isParsed).isFalse();

    project.includeTransitive();
    project.eagerlyParse(runnable -> {});
    assertThat(directAbc.isParsed).isTrue();
    assertThat(transitiveAbc.isParsed).isTrue();
  }
}
