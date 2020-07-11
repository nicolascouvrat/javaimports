package com.nikodoko.javaimports.resolver.maven;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.resolver.ImportWithDistance;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MavenDependencyResolverTest {
  static final URL repositoryURL = MavenDependencyResolverTest.class.getResource("/testrepository");
  MavenDependencyResolver resolver;

  @BeforeEach
  void setup() throws Exception {
    Path repository = Paths.get(repositoryURL.toURI());
    Path reference = repository;
    resolver = new MavenDependencyResolver(reference, repository);
  }

  @Test
  void testDependencyWithPlainVersionIsResolved() throws Exception {
    List<ImportWithDistance> expected =
        ImmutableList.of(new ImportWithDistance(new Import("App", "com.mycompany.app", false), 6));

    List<ImportWithDistance> got =
        resolver.resolve(
            ImmutableList.of(new MavenDependency("com.mycompany.app", "a-dependency", "1.0")));

    assertThat(got).containsExactlyElementsIn(expected);
  }

  @Test
  void testSubclassIsResolved() throws Exception {
    List<ImportWithDistance> expected =
        ImmutableList.of(
            new ImportWithDistance(new Import("Subclass", "com.mycompany.app.App", false), 6),
            new ImportWithDistance(
                new Import("Subsubclass", "com.mycompany.app.App.Subclass", false), 6),
            new ImportWithDistance(new Import("App", "com.mycompany.app", false), 6));

    List<ImportWithDistance> got =
        resolver.resolve(
            ImmutableList.of(new MavenDependency("com.mycompany.app", "a-dependency", "2.0")));

    assertThat(got).containsExactlyElementsIn(expected);
  }

  @Test
  void testDependencyWithoutPlainVersionIsResolvedToLatest() throws Exception {
    List<ImportWithDistance> expected =
        ImmutableList.of(
            new ImportWithDistance(new Import("Subclass", "com.mycompany.app.App", false), 6),
            new ImportWithDistance(
                new Import("Subsubclass", "com.mycompany.app.App.Subclass", false), 6),
            new ImportWithDistance(new Import("App", "com.mycompany.app", false), 6));

    List<ImportWithDistance> got =
        resolver.resolve(
            ImmutableList.of(new MavenDependency("com.mycompany.app", "a-dependency", null)));

    assertThat(got).containsExactlyElementsIn(expected);
  }
}
