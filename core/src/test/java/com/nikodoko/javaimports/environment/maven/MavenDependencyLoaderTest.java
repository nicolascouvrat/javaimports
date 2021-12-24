package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth.assertThat;
import static com.nikodoko.javaimports.common.CommonTestUtil.aSelector;
import static com.nikodoko.javaimports.common.CommonTestUtil.anImport;
import static com.nikodoko.javaimports.common.CommonTestUtil.someIdentifiers;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.environment.common.IdentifierLoader;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MavenDependencyLoaderTest {
  int loaderCalls;

  @BeforeEach
  void setup() {
    loaderCalls = 0;
  }

  IdentifierLoader dummyLoader(Map<Import, Set<Identifier>> identifiers) {
    return i -> {
      loaderCalls++;
      return identifiers.get(i);
    };
  }

  @Test
  void itShouldNotLoadIfAskingForNonExistingImport() {
    var dep =
        new MavenDependencyLoader.Dependency(
            Set.of(anImport("com.myapp.App")), dummyLoader(Map.of()));

    var got = dep.findClass(anImport("com.myapp.AnotherApp"));
    assertThat(got.isEmpty()).isTrue();
    assertThat(loaderCalls).isEqualTo(0);
  }

  @Test
  void itShouldLazyLoadIdentifiers() {
    var dep =
        new MavenDependencyLoader.Dependency(
            Set.of(anImport("com.myapp.App")),
            dummyLoader(Map.of(anImport("com.myapp.App"), someIdentifiers("a", "b", "c"))));

    assertThat(loaderCalls).isEqualTo(0);
  }

  @Test
  void itShouldCacheIdentifiers() {
    var expected =
        ClassEntity.named(aSelector("com.myapp.App"))
            .declaring(someIdentifiers("a", "b", "c"))
            .build();
    var dep =
        new MavenDependencyLoader.Dependency(
            Set.of(anImport("com.myapp.App")),
            dummyLoader(Map.of(anImport("com.myapp.App"), expected.declarations)));

    var got = dep.findClass(anImport("com.myapp.App"));
    assertThat(got.isPresent()).isTrue();
    assertThat(got.get()).isEqualTo(expected);
    assertThat(loaderCalls).isEqualTo(1);

    got = dep.findClass(anImport("com.myapp.App"));
    assertThat(got.isPresent()).isTrue();
    assertThat(got.get()).isEqualTo(expected);
    assertThat(loaderCalls).isEqualTo(1);
  }
}
