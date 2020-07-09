package com.nikodoko.javaimports.resolver;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.nikodoko.javaimports.parser.Import;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

class MavenDependencyResolverTest {
  static final URL repositoryURL = MavenDependencyResolverTest.class.getResource("/testrepository");

  @Test
  void testDependencyWithPlainVersionIsResolved() throws Exception {
    Path repository = Paths.get(repositoryURL.toURI());
    Path reference = repository;
    MavenDependencyResolver resolver = new MavenDependencyResolver(reference, repository);
    List<ImportWithDistance> expected =
        ImmutableList.of(new ImportWithDistance(new Import("com.mycompany.app", "App", false), 6));

    List<ImportWithDistance> got =
        resolver.resolve(
            ImmutableList.of(new MavenDependency("com.mycompany.app", "a-dependency", "1.0")));

    assertThat(got).containsExactlyElementsIn(expected);
  }
}
