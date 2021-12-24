package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth.assertThat;
import static com.nikodoko.javaimports.common.CommonTestUtil.anImport;

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

  IdentifierLoader dummyLoader(Map<Import, Set<String>> identifiers) {
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

    var got = dep.findIdentifiers(anImport("com.myapp.AnotherApp"));
    assertThat(got).isEmpty();
    assertThat(loaderCalls).isEqualTo(0);
  }

  @Test
  void itShouldLazyLoadIdentifiers() {
    var dep =
        new MavenDependencyLoader.Dependency(
            Set.of(anImport("com.myapp.App")),
            dummyLoader(Map.of(anImport("com.myapp.App"), Set.of("a", "b", "c"))));

    assertThat(loaderCalls).isEqualTo(0);
  }

  @Test
  void itShouldCacheIdentifiers() {
    var expected = Set.of("a", "b", "c");
    var dep =
        new MavenDependencyLoader.Dependency(
            Set.of(anImport("com.myapp.App")),
            dummyLoader(Map.of(anImport("com.myapp.App"), expected)));

    var got = dep.findIdentifiers(anImport("com.myapp.App"));
    assertThat(got).containsExactlyElementsIn(expected);
    assertThat(loaderCalls).isEqualTo(1);

    got = dep.findIdentifiers(anImport("com.myapp.App"));
    assertThat(got).containsExactlyElementsIn(expected);
    assertThat(loaderCalls).isEqualTo(1);
  }
}
