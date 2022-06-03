package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.DefaultModelWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MavenDependencyFinderTest {
  Path tmp;
  MavenDependencyFinder finder;

  @BeforeEach
  void setup() throws Exception {
    tmp = Files.createDirectory(Files.createTempDirectory("").resolve("module"));
    finder = new MavenDependencyFinder(new DummyRepository(Map.of()));
  }

  @Test
  void testThatPomWithNoDepenciesReturnsEmptyList() throws Exception {
    writeChild(basicPom());

    MavenDependencyFinder.Result got = finder.findAll(tmp);
    assertThat(got.dependencies).isEmpty();
    assertThat(got.errors).isEmpty();
  }

  @Test
  void testThatPomWithDependenciesReturnsCorrectDependencies() throws Exception {
    writeChild(
        basicPom(),
        withDependency("com.google.guava", "guava", "28.1-jre"),
        withDependency("com.google.truth", "truth", "1.0.1"));
    List<MavenDependency> expected =
        ImmutableList.of(
            dependencyWithDefaults("com.google.guava", "guava", "28.1-jre"),
            dependencyWithDefaults("com.google.truth", "truth", "1.0.1"));
    MavenDependencyFinder.Result got = finder.findAll(tmp);
    assertThat(got.dependencies).containsExactlyElementsIn(expected);
    assertThat(got.errors).isEmpty();
  }

  @Test
  void testDependenciesUsingParametersAreFoundEvenIfNotResolved() throws Exception {
    writeChild(basicPom(), withDependency("com.google.guava", "guava", "${guava.version}"));
    List<MavenDependency> expected =
        ImmutableList.of(dependencyWithDefaults("com.google.guava", "guava", "${guava.version}"));

    MavenDependencyFinder.Result got = finder.findAll(tmp);
    assertThat(got.dependencies).containsExactlyElementsIn(expected);
    assertThat(got.errors).isEmpty();
  }

  @Test
  void testThatPropertiesAreResolvedInTheSameFile() throws Exception {
    writeChild(
        basicPom(),
        withDependency("com.google.guava", "guava", "${guava.version}"),
        withProperty("guava.version", "28.1-jre"));
    var expected = List.of(dependencyWithDefaults("com.google.guava", "guava", "28.1-jre"));

    var got = finder.findAll(tmp);
    assertThat(got.errors).isEmpty();
    assertThat(got.dependencies).containsExactlyElementsIn(expected);
  }

  @Test
  void testThatFinderDoesNotCrashOnInvalidPom() throws Exception {
    Files.write(Paths.get(tmp.toString(), "pom.xml"), "this is not a valid pom!".getBytes());

    MavenDependencyFinder.Result got = finder.findAll(tmp);
    assertThat(got.dependencies).isEmpty();
    assertThat(got.errors).hasSize(1);
  }

  @Test
  void testThatExplicitParentPomIsFound() throws Exception {
    writeChild(
        basicPom(),
        withExplicitRelativePath("../pom.xml"),
        withDependency("com.google.guava", "guava", "${guava.version}"));
    writeParent(basicPom(), withProperty("guava.version", "28.1-jre"));
    var expected = List.of(dependencyWithDefaults("com.google.guava", "guava", "28.1-jre"));

    var got = finder.findAll(tmp);
    assertThat(got.errors).isEmpty();
    assertThat(got.dependencies).containsExactlyElementsIn(expected);
  }

  @Test
  void testThatParentPomIsFoundIfOnlyDirectory() throws Exception {
    writeChild(
        basicPom(),
        withExplicitRelativePath(".."),
        withDependency("com.google.guava", "guava", "${guava.version}"));
    writeParent(basicPom(), withProperty("guava.version", "28.1-jre"));
    var expected = List.of(dependencyWithDefaults("com.google.guava", "guava", "28.1-jre"));

    var got = finder.findAll(tmp);
    assertThat(got.errors).isEmpty();
    assertThat(got.dependencies).containsExactlyElementsIn(expected);
  }

  @Test
  void theThatNoParentPomIsFoundIfEmptyRelativePath() throws Exception {
    writeChild(
        basicPom(),
        withExplicitRelativePath(""),
        withDependency("com.google.guava", "guava", "${guava.version}"));
    writeParent(basicPom(), withProperty("guava.version", "28.1-jre"));
    var expected = List.of(dependencyWithDefaults("com.google.guava", "guava", "${guava.version}"));

    var got = finder.findAll(tmp);
    assertThat(got.errors).isEmpty();
    assertThat(got.dependencies).containsExactlyElementsIn(expected);
  }

  @Test
  void testThatImplicitParentPomIsFound() throws Exception {
    writeChild(
        basicPom(),
        withImplicitRelativePath(),
        withDependency("com.google.guava", "guava", "${guava.version}"));
    writeParent(basicPom(), withProperty("guava.version", "28.1-jre"));
    var expected = List.of(dependencyWithDefaults("com.google.guava", "guava", "28.1-jre"));

    var got = finder.findAll(tmp);
    assertThat(got.errors).isEmpty();
    assertThat(got.dependencies).containsExactlyElementsIn(expected);
  }

  @Test
  void testThatDependencyTypeScopeAndOptionalAreExtracted() throws Exception {
    writeChild(
        basicPom(),
        withDependency("com.google.guava", "guava", "28.1-jre", "test", "provided", true));
    var expected =
        List.of(
            new MavenDependency("com.google.guava", "guava", "28.1-jre", "test", "provided", true));

    var got = finder.findAll(tmp);
    assertThat(got.errors).isEmpty();
    assertThat(got.dependencies).containsExactlyElementsIn(expected);
  }

  @Test
  void testThatManagedDependencyWithScopeImportIsReplacedWithItsManagedDependencies()
      throws Exception {
    var repository =
        new DummyRepository(
            Map.of(
                new MavenDependency("com.test", "test-bom", "1.0", "pom", "import", false),
                List.of(dependencyWithDefaults("com.test", "mydependency", "1.0"))));
    var finder = new MavenDependencyFinder(repository);
    writeChild(
        basicPom(),
        withDependency("com.test", "mydependency", null),
        withManagedDependency("com.test", "test-bom", "1.0", "pom", "import", false));
    var expected = dependencyWithDefaults("com.test", "mydependency", "1.0");

    var got = finder.findAll(tmp);
    assertThat(got.errors).isEmpty();
    assertThat(got.dependencies).containsExactly(expected);
  }

  static Consumer<Model> basicPom() {
    return m -> {
      m.setModelVersion("4.0.0");
      m.setGroupId("com.nikodoko.javaimports");
      m.setArtifactId("test-pom");
      m.setVersion("0.0");
      m.setDependencyManagement(new DependencyManagement());
    };
  }

  static Consumer<Model> withManagedDependency(Object... elements) {
    var managedDep = mvnDependency(elements);

    return m -> {
      var dependencyManagement = m.getDependencyManagement();
      dependencyManagement.addDependency(managedDep);
    };
  }

  static Consumer<Model> withDependency(Object... elements) {
    var dep = mvnDependency(elements);

    return m -> {
      var deps = m.getDependencies();
      deps.add(dep);
      m.setDependencies(deps);
    };
  }

  // Either 3 elements (maven coordinates) or 6 (adding type, scope and optional)
  static Dependency mvnDependency(Object... elements) {
    Dependency dep = new Dependency();
    dep.setGroupId((String) elements[0]);
    dep.setArtifactId((String) elements[1]);
    dep.setVersion((String) elements[2]);
    if (elements.length > 3) {
      dep.setType((String) elements[3]);
      dep.setScope((String) elements[4]);
      dep.setOptional((Boolean) elements[5]);
    }

    return dep;
  }

  static Consumer<Model> withProperty(String key, String value) {
    return m -> m.addProperty(key, value);
  }

  static Consumer<Model> withExplicitRelativePath(String relativePath) {
    var parent = new Parent();
    parent.setGroupId("com.nikodoko.javaimports");
    parent.setArtifactId("test-pom-parent");
    parent.setVersion("0.0");
    parent.setRelativePath(relativePath);

    return m -> m.setParent(parent);
  }

  static Consumer<Model> withImplicitRelativePath() {
    return withExplicitRelativePath(null);
  }

  void writeChild(Consumer<Model>... options) throws Exception {
    write(tmp, options);
  }

  void writeParent(Consumer<Model>... options) throws Exception {
    write(tmp.getParent(), options);
  }

  void write(Path dir, Consumer<Model>... options) throws Exception {
    Model pom = new Model();
    for (Consumer<Model> opt : options) {
      opt.accept(pom);
    }

    File target = Paths.get(dir.toString(), "pom.xml").toFile();
    new DefaultModelWriter().write(target, null, pom);
  }

  static MavenDependency dependencyWithDefaults(String groupId, String artifactId, String version) {
    return new MavenDependency(groupId, artifactId, version, "jar", null, false);
  }

  static class DummyRepository implements MavenRepository {
    private final Map<MavenDependency, List<MavenDependency>> managedDeps;

    DummyRepository(Map<MavenDependency, List<MavenDependency>> managedDeps) {
      this.managedDeps = managedDeps;
    }

    @Override
    public List<MavenDependency> getManagedDependencies(MavenDependency dep) {
      return managedDeps.getOrDefault(dep, List.of());
    }
  }
}
