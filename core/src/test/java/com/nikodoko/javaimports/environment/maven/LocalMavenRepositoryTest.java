package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth.assertThat;

import com.nikodoko.javaimports.Options;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LocalMavenRepositoryTest {
  static final URL rootURL =
      LocalMavenRepositoryTest.class.getResource("/fixtures/unittests/javaimports-pom/core");
  static final URL repositoryURL = MavenDependencyLoaderTest.class.getResource("/testrepository");

  LocalMavenRepository repository;

  @BeforeEach
  void setup() throws Exception {
    var repositoryPath = Paths.get(repositoryURL.toURI());
    var resolver = MavenDependencyResolver.withRepository(repositoryPath);
    var options = Options.builder().debug(true).build();
    repository = new LocalMavenRepository(resolver, options);
  }

  @Test
  void itShouldGetManagedDependencies() {
    var target = aDependency("org.junit.jupiter:junit-jupiter:jar:5.6.2");
    var expected =
        List.of(
            aDependency("org.junit.jupiter:junit-jupiter:jar:5.6.2"),
            aDependency("org.junit.jupiter:junit-jupiter-api:jar:5.6.2"),
            aDependency("org.junit.jupiter:junit-jupiter-engine:jar:5.6.2"),
            aDependency("org.junit.jupiter:junit-jupiter-migrationsupport:jar:5.6.2"),
            aDependency("org.junit.jupiter:junit-jupiter-params:jar:5.6.2"),
            aDependency("org.junit.platform:junit-platform-commons:jar:1.6.2"),
            aDependency("org.junit.platform:junit-platform-console:jar:1.6.2"),
            aDependency("org.junit.platform:junit-platform-engine:jar:1.6.2"),
            aDependency("org.junit.platform:junit-platform-launcher:jar:1.6.2"),
            aDependency("org.junit.platform:junit-platform-reporting:jar:1.6.2"),
            aDependency("org.junit.platform:junit-platform-runner:jar:1.6.2"),
            aDependency("org.junit.platform:junit-platform-suite-api:jar:1.6.2"),
            aDependency("org.junit.platform:junit-platform-testkit:jar:1.6.2"),
            aDependency("org.junit.vintage:junit-vintage-engine:jar:5.6.2"));
    var got = repository.getManagedDependencies(target);
    // TODO: this should be "exactlyElementsIn" but for now we also return the original dependency
    // of scope import
    assertThat(got).containsAtLeastElementsIn(expected);
  }

  @Test
  void itShouldResolveTransitiveDependenciesWithMultipleParentPoms() {
    var target = aDependency("javax.enterprise:cdi-api:jar:1.0:compile");
    var expected =
        List.of(
            aDependency("org.jboss.interceptor:jboss-interceptor-api:jar:1.1"),
            aDependency("javax.annotation:jsr250-api:jar:1.0"),
            aDependency("javax.inject:javax.inject:jar:1"));
    var got = repository.getTransitiveDependencies(List.of(target), 1);
    assertThat(got).containsExactlyElementsIn(expected);
  }

  static String[] EXPECTED_DEPENDENCIES = {
    "org.apache.maven:maven-core:jar:3.6.3:compile",
    "org.apache.maven:maven-model:jar:3.6.3:compile",
    "org.apache.maven:maven-settings:jar:3.6.3:compile",
    "org.apache.maven:maven-settings-builder:jar:3.6.3:compile",
    "org.codehaus.plexus:plexus-interpolation:jar:1.25:compile",
    "org.sonatype.plexus:plexus-sec-dispatcher:jar:1.4:compile",
    "org.sonatype.plexus:plexus-cipher:jar:1.4:compile",
    "org.apache.maven:maven-builder-support:jar:3.6.3:compile",
    "org.apache.maven:maven-repository-metadata:jar:3.6.3:compile",
    "org.apache.maven:maven-artifact:jar:3.6.3:compile",
    "org.apache.maven:maven-plugin-api:jar:3.6.3:compile",
    "org.apache.maven:maven-model-builder:jar:3.6.3:compile",
    "org.apache.maven:maven-resolver-provider:jar:3.6.3:compile",
    "org.apache.maven.resolver:maven-resolver-impl:jar:1.4.1:compile",
    "org.apache.maven.resolver:maven-resolver-api:jar:1.4.1:compile",
    "org.apache.maven.resolver:maven-resolver-spi:jar:1.4.1:compile",
    "org.apache.maven.resolver:maven-resolver-util:jar:1.4.1:compile",
    "org.apache.maven.shared:maven-shared-utils:jar:3.2.1:compile",
    "commons-io:commons-io:jar:2.5:compile",
    "org.eclipse.sisu:org.eclipse.sisu.plexus:jar:0.3.4:compile",
    "javax.enterprise:cdi-api:jar:1.0:compile",
    "javax.annotation:jsr250-api:jar:1.0:compile",
    "org.eclipse.sisu:org.eclipse.sisu.inject:jar:0.3.4:compile",
    "com.google.inject:guice:jar:no_aop:4.2.1:compile",
    "aopalliance:aopalliance:jar:1.0:compile",
    "javax.inject:javax.inject:jar:1:compile",
    "org.codehaus.plexus:plexus-utils:jar:3.2.1:compile",
    "org.codehaus.plexus:plexus-classworlds:jar:2.6.0:compile",
    "org.codehaus.plexus:plexus-component-annotations:jar:2.1.0:compile",
    "org.apache.commons:commons-lang3:jar:3.8.1:compile",
    "com.google.guava:guava:jar:28.1-jre:compile",
    "com.google.guava:failureaccess:jar:1.0.1:compile",
    "com.google.guava:listenablefuture:jar:9999.0-empty-to-avoid-conflict-with-guava:compile",
    "com.google.code.findbugs:jsr305:jar:3.0.2:compile",
    "org.checkerframework:checker-qual:jar:2.8.1:compile",
    "com.google.errorprone:error_prone_annotations:jar:2.3.2:compile",
    "com.google.j2objc:j2objc-annotations:jar:1.3:compile",
    "org.codehaus.mojo:animal-sniffer-annotations:jar:1.18:compile",
    "com.google.googlejavaformat:google-java-format:jar:1.9:compile",
    "com.datadoghq:java-dogstatsd-client:jar:4.0.0:compile",
    "com.github.jnr:jnr-unixsocket:jar:0.36:compile",
    "com.github.jnr:jnr-ffi:jar:2.1.16:compile",
    "com.github.jnr:jffi:jar:1.2.23:compile",
    "com.github.jnr:jffi:jar:native:1.2.23:runtime",
    "org.ow2.asm:asm:jar:7.1:compile",
    "org.ow2.asm:asm-commons:jar:7.1:compile",
    "org.ow2.asm:asm-analysis:jar:7.1:compile",
    "org.ow2.asm:asm-tree:jar:7.1:compile",
    "org.ow2.asm:asm-util:jar:7.1:compile",
    "com.github.jnr:jnr-a64asm:jar:1.0.0:compile",
    "com.github.jnr:jnr-x86asm:jar:1.0.2:compile",
    "com.github.jnr:jnr-constants:jar:0.9.17:compile",
    "com.github.jnr:jnr-enxio:jar:0.30:compile",
    "com.github.jnr:jnr-posix:jar:3.0.61:compile",
    "io.opentracing:opentracing-api:jar:0.32.0:compile",
    "io.opentracing:opentracing-util:jar:0.32.0:compile",
    "io.opentracing:opentracing-noop:jar:0.32.0:compile",
    "com.datadoghq:dd-trace-api:jar:0.101.0:compile",
    "com.datadoghq:dd-trace-ot:jar:0.101.0:compile",
    "io.opentracing.contrib:opentracing-tracerresolver:jar:0.1.0:compile",
    "org.slf4j:slf4j-api:jar:1.7.30:compile",
    "com.squareup.okhttp3:okhttp:jar:3.12.12:compile",
    "com.squareup.okio:okio:jar:1.16.0:compile",
    "com.squareup.moshi:moshi:jar:1.9.2:compile",
    "org.jctools:jctools-core:jar:3.3.0:compile",
    "com.datadoghq:sketches-java:jar:0.8.2:compile",
    "org.slf4j:slf4j-nop:jar:1.7.30:compile",
    "net.jqwik:jqwik:jar:1.3.1:test",
    "org.apiguardian:apiguardian-api:jar:1.1.0:test",
    "net.jqwik:jqwik-api:jar:1.3.1:test",
    "org.opentest4j:opentest4j:jar:1.2.0:test",
    "org.junit.platform:junit-platform-commons:jar:1.6.2:test",
    "net.jqwik:jqwik-engine:jar:1.3.1:test",
    "org.junit.platform:junit-platform-engine:jar:1.6.2:test",
    "com.google.truth:truth:jar:1.0.1:test",
    "org.checkerframework:checker-compat-qual:jar:2.5.5:test",
    "junit:junit:jar:4.12:test",
    "org.hamcrest:hamcrest-core:jar:1.3:test",
    "com.googlecode.java-diff-utils:diffutils:jar:1.3.0:test",
    "com.google.auto.value:auto-value-annotations:jar:1.6.3:test",
    "org.junit.jupiter:junit-jupiter:jar:5.6.2:test",
    "org.junit.jupiter:junit-jupiter-api:jar:5.6.2:test",
    "org.junit.jupiter:junit-jupiter-engine:jar:5.6.2:test",
    "org.junit.jupiter:junit-jupiter-params:jar:5.6.2:test",
    "com.google.truth.extensions:truth-java8-extension:jar:1.0.1:test",
    "com.nikodoko.javapackagetest:javapackagetest:jar:1.0:test"
  };

  @Test
  void itShouldGetAllTransitiveDependenciesForJavaimports() throws Exception {
    var expectedDependencies =
        Arrays.stream(EXPECTED_DEPENDENCIES)
            .map(LocalMavenRepositoryTest::aDependency)
            .map(MavenDependency::coordinates)
            .collect(Collectors.toList());
    var target = aDependency("com.nikodoko.javaimports:javaimports:jar:1.3");
    var direct = repository.getDirectDependencies(target);
    var transitive = repository.getTransitiveDependencies(direct, -1);
    var gotCoord =
        Stream.concat(direct.stream(), transitive.stream())
            .map(MavenDependency::coordinates)
            .collect(Collectors.toList());

    assertThat(gotCoord).containsAtLeastElementsIn(expectedDependencies);
  }

  // [groupId]:[artifactId]:[type]:[version]
  private static MavenDependency aDependency(String depString) {
    var elements = depString.split(":");
    assert elements.length >= 4;
    var groupId = elements[0];
    var artifactId = elements[1];
    var type = elements[2];
    var version = elements[3];
    String classifier = null;
    if (elements.length == 6) {
      classifier = elements[3];
      version = elements[4];
    }
    return new MavenDependency(groupId, artifactId, version, type, classifier, null, false);
  }
}
