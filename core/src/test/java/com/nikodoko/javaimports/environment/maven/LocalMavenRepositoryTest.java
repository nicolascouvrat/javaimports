package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth.assertThat;

import com.nikodoko.javaimports.Options;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
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

  @Test
  void itShouldProperlyApplyExclusions() {
    var target =
        aDependencyWithExclusions(
            "javax.enterprise:cdi-api:jar:1.0:compile",
            "javax.el:el-api",
            "org.jboss.interceptor:jboss-interceptor-api");
    var expected =
        List.of(
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
  void itShouldGetExactlyAllTransitiveDependenciesForJavaimports() throws Exception {
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

    assertThat(gotCoord).containsExactlyElementsIn(expectedDependencies);
  }

  private static MavenDependency aDependency(String depString) {
    return aDependencyWithExclusions(depString);
  }

  // [groupId]:[artifactId]:[type]:[version], then [groupId]:[artifactId]
  private static MavenDependency aDependencyWithExclusions(
      String depString, String... exclusionStrings) {
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

    List<MavenDependency.Exclusion> exclusions = new ArrayList<>();
    for (var exclusionString : exclusionStrings) {
      elements = exclusionString.split(":");
      assert elements.length == 2;
      exclusions.add(new MavenDependency.Exclusion(elements[0], elements[1]));
    }

    return new MavenDependency(
        groupId, artifactId, version, type, classifier, null, false, exclusions);
  }

  String[] INDEXATION_DEPS =
      new String[] {
        "com.fsmatic:event-platform-facets:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:streaming-engine-temporary-rule-engine:jar:1.0-SNAPSHOT:compile",
        "org.slf4j:slf4j-api:jar:1.7.30:compile",
        "com.fsmatic:logs-backend-es6-shaded:jar:6.8.6-opendistro-5fed456b:compile",
        "com.fsmatic:logs-backend-es7-shaded:jar:7.9.1-opendistro-5fed456b:compile",
        "io.dropwizard.metrics:metrics-core:jar:3.2.2:compile",
        "com.fsmatic:logs-backend-workload:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-routes:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:koutris-client:jar:1.0-SNAPSHOT:compile",
        "com.datadoghq:dd-trace-api:jar:0.101.0:compile",
        "com.fsmatic:logs-backend-context-loader-grpc:jar:1.0-SNAPSHOT:compile",
        "com.salesforce.servicelibs:reactor-grpc-stub:jar:1.0.1:compile",
        "com.salesforce.servicelibs:reactive-grpc-common:jar:1.0.1:compile",
        "com.fsmatic:logs-backend-pb:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-workload-netty-shaded:jar:4.1.43.Final-5fed456b:compile",
        "com.dd.libstreaming:libstreaming:jar:e86b6578:compile",
        "org.apache.curator:curator-recipes:jar:5.2.0:compile",
        "org.apache.zookeeper:zookeeper:jar:3.5.8:compile",
        "org.apache.yetus:audience-annotations:jar:0.5.0:compile",
        "io.netty:netty-handler:jar:4.1.77.Final:compile",
        "io.netty:netty-common:jar:4.1.77.Final:compile",
        "io.netty:netty-resolver:jar:4.1.77.Final:compile",
        "io.netty:netty-buffer:jar:4.1.77.Final:compile",
        "io.netty:netty-transport:jar:4.1.77.Final:compile",
        "io.netty:netty-codec:jar:4.1.77.Final:compile",
        "io.netty:netty-transport-native-epoll:jar:4.1.77.Final:compile",
        "io.netty:netty-transport-native-unix-common:jar:4.1.77.Final:compile",
        "io.netty:netty-transport-classes-epoll:jar:4.1.77.Final:compile",
        "org.apache.zookeeper:zookeeper-jute:jar:3.5.8:compile",
        "org.jetbrains:annotations:jar:17.0.0:compile",
        "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:jar:2.12.0:compile",
        "net.openhft:zero-allocation-hashing:jar:0.12:compile",
        "com.fasterxml.jackson.dataformat:jackson-dataformat-smile:jar:2.12.0:compile",
        "com.google.protobuf:protobuf-java-util:jar:3.19.2:compile",
        "io.grpc:grpc-netty-shaded:jar:1.45.1:compile",
        "org.apache.curator:curator-framework:jar:5.2.0:compile",
        "org.apache.commons:commons-lang3:jar:3.11:compile",
        "com.google.api.grpc:proto-google-common-protos:jar:1.17.0:compile",
        "com.fsmatic:logs-backend-koutris-grpc:jar:1.0-SNAPSHOT:compile",
        "org.msgpack:jackson-dataformat-msgpack:jar:0.8.20:compile",
        "org.agrona:agrona:jar:1.7.2:compile",
        "ch.qos.logback:logback-classic:jar:1.2.3:compile",
        "org.scala-lang.modules:scala-java8-compat_2.12:jar:0.9.0:compile",
        "com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:jar:2.12.0:compile",
        "commons-lang:commons-lang:jar:2.6:compile",
        "commons-collections:commons-collections:jar:3.2.2:compile",
        "io.grpc:grpc-protobuf:jar:1.45.1:compile",
        "io.grpc:grpc-protobuf-lite:jar:1.45.1:compile",
        "org.apache.commons:commons-collections4:jar:4.4:compile",
        "org.apache.curator:curator-client:jar:5.2.0:compile",
        "com.fsmatic:logs-backend-storage-cells:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-rum-commons-workload:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-context-loader-blob-client:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-feature-flags-grpc:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-feature-flags:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-sharding-allocator-grpc:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-sharding-balancer-grpc:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-object-storage-provider:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-aws-tools-v2:jar:1.0-SNAPSHOT:compile",
        "software.amazon.awssdk:ec2:jar:2.16.104:compile",
        "software.amazon.awssdk:aws-query-protocol:jar:2.16.104:compile",
        "software.amazon.awssdk:sts:jar:2.16.104:compile",
        "software.amazon.awssdk:aws-core:jar:2.16.104:compile",
        "io.netty:netty-tcnative-boringssl-static:jar:2.0.48.Final:compile",
        "io.netty:netty-tcnative-classes:jar:2.0.48.Final:compile",
        "software.amazon.awssdk:profiles:jar:2.16.104:compile",
        "com.fsmatic:logs-backend-gcp-tools:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-azure-tools:jar:1.0-SNAPSHOT:compile",
        "software.amazon.awssdk:auth:jar:2.16.104:compile",
        "software.amazon.awssdk:annotations:jar:2.16.104:compile",
        "software.amazon.awssdk:utils:jar:2.16.104:compile",
        "software.amazon.eventstream:eventstream:jar:1.0.1:compile",
        "org.threeten:threetenbp:jar:1.5.1:compile",
        "com.fsmatic:logs-backend-azure-sdk-shaded:jar:12.14.3-5fed456b:compile",
        "software.amazon.awssdk:regions:jar:2.16.104:compile",
        "software.amazon.awssdk:s3:jar:2.16.104:compile",
        "software.amazon.awssdk:aws-xml-protocol:jar:2.16.104:compile",
        "software.amazon.awssdk:protocol-core:jar:2.16.104:compile",
        "software.amazon.awssdk:arns:jar:2.16.104:compile",
        "software.amazon.awssdk:metrics-spi:jar:2.16.104:compile",
        "com.google.cloud:google-cloud-storage:jar:1.93.0:compile",
        "com.google.cloud:google-cloud-core-http:jar:1.90.0:compile",
        "com.google.api-client:google-api-client:jar:1.30.2:compile",
        "com.google.oauth-client:google-oauth-client:jar:1.30.1:compile",
        "com.google.http-client:google-http-client-jackson2:jar:1.41.2:compile",
        "com.google.http-client:google-http-client-appengine:jar:1.41.2:compile",
        "com.google.api:gax-httpjson:jar:0.64.1:compile",
        "com.google.apis:google-api-services-storage:jar:v1-rev20190910-1.30.3:compile",
        "com.google.cloud:google-cloud-core:jar:1.90.0:compile",
        "com.google.api.grpc:proto-google-iam-v1:jar:0.12.0:compile",
        "com.google.api:gax:jar:2.4.0:compile",
        "com.google.api:api-common:jar:1.8.1:compile",
        "io.opencensus:opencensus-api:jar:0.21.0:compile",
        "software.amazon.awssdk:sdk-core:jar:2.16.104:compile",
        "software.amazon.awssdk:apache-client:jar:2.16.104:compile",
        "org.apache.httpcomponents:httpclient:jar:4.5.13:compile",
        "org.apache.httpcomponents:httpcore:jar:4.4.13:compile",
        "software.amazon.awssdk:http-client-spi:jar:2.16.104:compile",
        "software.amazon.awssdk:netty-nio-client:jar:2.16.104:compile",
        "io.netty:netty-codec-http:jar:4.1.77.Final:compile",
        "io.netty:netty-codec-http2:jar:4.1.77.Final:compile",
        "io.netty:netty-transport-native-epoll:jar:linux-x86_64:4.1.77.Final:compile",
        "com.typesafe.netty:netty-reactive-streams-http:jar:2.0.5:compile",
        "com.typesafe.netty:netty-reactive-streams:jar:2.0.5:compile",
        "com.fsmatic:logs-backend-rocksdb:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:processing-model:jar:1.0-SNAPSHOT:compile",
        "io.projectreactor:reactor-core:jar:3.4.14:compile",
        "org.rocksdb:rocksdbjni:jar:6.27.3:compile",
        "com.fsmatic:event-to-metrics-model:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-object-storage:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-external-archive-lib:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:enclave-client:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-enclave-grpc:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-dd-internal-authentication:jar:1.0-SNAPSHOT:compile",
        "io.github.resilience4j:resilience4j-retry:jar:1.7.0:compile",
        "io.github.resilience4j:resilience4j-core:jar:1.7.0:compile",
        "io.vavr:vavr:jar:0.10.2:compile",
        "io.vavr:vavr-match:jar:0.10.2:compile",
        "com.amazonaws:aws-java-sdk-core:jar:1.12.49:compile",
        "commons-logging:commons-logging:jar:1.2:compile",
        "software.amazon.ion:ion-java:jar:1.0.2:compile",
        "com.google.auth:google-auth-library-credentials:jar:1.4.0:compile",
        "com.google.auth:google-auth-library-oauth2-http:jar:1.4.0:compile",
        "com.google.auto.value:auto-value-annotations:jar:1.8.2:compile",
        "com.google.http-client:google-http-client:jar:1.41.2:compile",
        "io.opencensus:opencensus-contrib-http-util:jar:0.30.0:compile",
        "com.google.http-client:google-http-client-gson:jar:1.41.0:compile",
        "com.fsmatic:logs-backend-grpc-utils:jar:1.0-SNAPSHOT:compile",
        "com.hubspot.jackson:jackson-datatype-protobuf:jar:0.9.11-jackson2.9:compile",
        "io.opentracing.contrib:opentracing-grpc:jar:0.1.2:compile",
        "io.grpc:grpc-services:jar:1.45.1:compile",
        "io.micrometer:micrometer-core:jar:1.8.1:compile",
        "org.latencyutils:LatencyUtils:jar:2.0.3:runtime",
        "io.grpc:grpc-context:jar:1.45.1:compile",
        "com.fsmatic:logs-backend-domain:jar:1.0-SNAPSHOT:compile",
        "org.mongodb:mongodb-driver-sync:jar:4.2.0:compile",
        "org.msgpack:msgpack-core:jar:0.8.20:compile",
        "org.checkerframework:checker-qual:jar:3.5.0:compile",
        "javax.validation:validation-api:jar:1.1.0.Final:compile",
        "com.fsmatic:processing-integrations:jar:1.0-SNAPSHOT:compile",
        "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:jar:2.12.0:compile",
        "org.yaml:snakeyaml:jar:1.27:compile",
        "com.fsmatic:logs-backend-search:jar:1.0-SNAPSHOT:compile",
        "org.javassist:javassist:jar:3.27.0-GA:compile",
        "com.fsmatic:logs-backend-wrapped-es6:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-wrapped-es7:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-sharding-allocator-client:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-indexing-service:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-indexing-service-client:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-platform-config-loader-client:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-platform-api-grpc:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-indexing-coordinator-client:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-indexing-coordinator-grpc:jar:1.0-SNAPSHOT:compile",
        "io.kubernetes:client-java-api:jar:11.0.4:compile",
        "io.swagger:swagger-annotations:jar:1.5.22:compile",
        "com.squareup.okhttp3:logging-interceptor:jar:3.14.9:compile",
        "io.gsonfire:gson-fire:jar:1.8.5:compile",
        "joda-time:joda-time:jar:2.10.6:compile",
        "org.joda:joda-convert:jar:2.2.1:compile",
        "com.fsmatic:logs-backend-registry:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-context-loader-client:jar:1.0-SNAPSHOT:compile",
        "org.reactivestreams:reactive-streams:jar:1.0.3:compile",
        "com.fsmatic:logs-backend-sharding-balancer-client:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-context-loader-grpc-client:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-context-writer-client:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-usage-reader:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:event-platform-usage-commons:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:event-platform-usage-tracker-client:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-wrapped-es:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-indexing-autosharder:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-indexing-autosharder-grpc:jar:1.0-SNAPSHOT:compile",
        "com.typesafe.akka:akka-testkit_2.12:jar:2.5.32:test",
        "org.mockito:mockito-core:jar:3.5.11:test",
        "net.bytebuddy:byte-buddy:jar:1.10.13:test",
        "net.bytebuddy:byte-buddy-agent:jar:1.10.13:test",
        "org.objenesis:objenesis:jar:3.1:test",
        "org.mockito:mockito-junit-jupiter:jar:3.5.11:test",
        "com.fsmatic:logs-backend-mongo:test-jar:tests:1.0-SNAPSHOT:test",
        "de.undercouch:bson4jackson:jar:2.11.0:compile",
        "org.mongodb:mongodb-driver-reactivestreams:jar:4.2.0:compile",
        "de.flapdoodle.embed:de.flapdoodle.embed.mongo:jar:3.3.0:compile",
        "de.flapdoodle.embed:de.flapdoodle.embed.process:jar:3.1.6:compile",
        "org.apache.commons:commons-compress:jar:1.21:compile",
        "net.java.dev.jna:jna:jar:5.10.0:compile",
        "net.java.dev.jna:jna-platform:jar:5.10.0:compile",
        "de.flapdoodle:de.flapdoodle.os:jar:1.1.3:compile",
        "com.fsmatic:logs-backend-context-loader:jar:1.0-SNAPSHOT:test",
        "com.fsmatic:logs-backend-feature-flags-client:jar:1.0-SNAPSHOT:test",
        "io.projectreactor:reactor-tools:jar:3.4.14:test",
        "com.fsmatic:logs-backend-context-reader:jar:1.0-SNAPSHOT:test",
        "com.fsmatic:logs-backend-routing:jar:1.0-SNAPSHOT:test",
        "com.fsmatic:logs-backend-context-writer:jar:1.0-SNAPSHOT:test",
        "com.fsmatic:logs-backend-platform-api:jar:1.0-SNAPSHOT:test",
        "com.fsmatic:logs-backend-context-publisher:jar:1.0-SNAPSHOT:test",
        "com.fsmatic:logs-backend-context-loader-blob-publisher:jar:1.0-SNAPSHOT:test",
        "com.fsmatic:logs-backend-context-publisher:test-jar:tests:1.0-SNAPSHOT:test",
        "com.fsmatic:logs-backend-context-loader:test-jar:tests:1.0-SNAPSHOT:test",
        "com.fsmatic:logs-backend-commons:test-jar:tests:1.0-SNAPSHOT:test",
        "com.typesafe.akka:akka-slf4j_2.12:jar:2.5.32:compile",
        "com.fasterxml.jackson.datatype:jackson-datatype-jdk8:jar:2.12.0:compile",
        "org.slf4j:jul-to-slf4j:jar:1.7.30:compile",
        "com.fsmatic:logs-backend-workload:test-jar:tests:1.0-SNAPSHOT:test",
        "com.fsmatic:logs-backend-service:test-jar:tests:1.0-SNAPSHOT:test",
        "com.fsmatic:logs-backend-hms-resolver-grpc:jar:1.0-SNAPSHOT:compile",
        "io.kubernetes:client-java:jar:11.0.4:compile",
        "io.prometheus:simpleclient:jar:0.9.0:compile",
        "io.prometheus:simpleclient_httpserver:jar:0.9.0:compile",
        "io.prometheus:simpleclient_common:jar:0.9.0:compile",
        "io.kubernetes:client-java-proto:jar:11.0.4:compile",
        "commons-codec:commons-codec:jar:1.15:compile",
        "commons-io:commons-io:jar:2.8.0:compile",
        "org.bouncycastle:bcprov-ext-jdk15on:jar:1.69:compile",
        "org.bouncycastle:bcpkix-jdk15on:jar:1.69:compile",
        "org.bouncycastle:bcprov-jdk15on:jar:1.69:compile",
        "org.bouncycastle:bcutil-jdk15on:jar:1.69:compile",
        "com.fsmatic:logs-vault-client:jar:1.0-SNAPSHOT:compile",
        "io.opentracing.contrib:opentracing-jaxrs2:jar:0.5.0:compile",
        "io.opentracing.contrib:opentracing-web-servlet-filter:jar:0.3.0:compile",
        "io.opentracing.contrib:opentracing-concurrent:jar:0.3.0:compile",
        "org.eclipse.microprofile.opentracing:microprofile-opentracing-api:jar:1.3:compile",
        "org.osgi:org.osgi.annotation.versioning:jar:1.0.0:compile",
        "io.opentracing.contrib:opentracing-okhttp3:jar:2.0.1:compile",
        "org.glassfish.jersey.containers:jersey-container-servlet:jar:2.26:compile",
        "org.glassfish.jersey.core:jersey-common:jar:2.26:compile",
        "org.glassfish.hk2:osgi-resource-locator:jar:1.0.1:compile",
        "org.glassfish.jersey.core:jersey-server:jar:2.26:compile",
        "org.glassfish.jersey.media:jersey-media-jaxb:jar:2.26:compile",
        "org.eclipse.jetty:jetty-server:jar:9.4.14.v20181114:compile",
        "org.eclipse.jetty:jetty-servlet:jar:9.4.14.v20181114:compile",
        "org.eclipse.jetty:jetty-security:jar:9.4.14.v20181114:compile",
        "org.eclipse.jetty:jetty-proxy:jar:9.4.14.v20181114:compile",
        "org.eclipse.jetty:jetty-client:jar:9.4.14.v20181114:compile",
        "org.eclipse.jetty:jetty-servlets:jar:9.4.14.v20181114:compile",
        "org.eclipse.jetty:jetty-continuation:jar:9.4.14.v20181114:compile",
        "javax.annotation:javax.annotation-api:jar:1.3.2:compile",
        "io.dropwizard.metrics:metrics-jetty9:jar:3.2.2:compile",
        "com.datadoghq:datadog-api-client:jar:1.2.0:compile",
        "org.glassfish.jersey.connectors:jersey-apache-connector:jar:2.27:compile",
        "org.glassfish.jersey.inject:jersey-hk2:jar:2.27:compile",
        "org.glassfish.hk2:hk2-locator:jar:2.5.0-b42:compile",
        "org.glassfish.jersey.media:jersey-media-multipart:jar:2.27:compile",
        "org.jvnet.mimepull:mimepull:jar:1.9.6:compile",
        "org.glassfish.jersey.media:jersey-media-json-jackson:jar:2.26:compile",
        "org.glassfish.jersey.ext:jersey-entity-filtering:jar:2.26:compile",
        "com.fasterxml.jackson.module:jackson-module-jaxb-annotations:jar:2.12.0:compile",
        "jakarta.xml.bind:jakarta.xml.bind-api:jar:2.3.2:compile",
        "jakarta.activation:jakarta.activation-api:jar:1.2.1:compile",
        "org.openapitools:jackson-databind-nullable:jar:0.2.0:compile",
        "com.github.scribejava:scribejava-core:jar:8.0.0:compile",
        "org.tomitribe:tomitribe-http-signatures:jar:1.3:compile",
        "com.squareup.okio:okio:jar:1.17.2:compile",
        "org.lz4:lz4-java:jar:1.6.0:compile",
        "com.github.luben:zstd-jni:jar:1.5.2-2:compile",
        "io.micrometer:micrometer-registry-statsd:jar:1.8.1:compile",
        "org.glassfish.jersey.containers:jersey-container-servlet-core:jar:2.26:compile",
        "org.glassfish.hk2.external:javax.inject:jar:2.5.0-b42:compile",
        "org.glassfish.hk2:hk2-api:jar:2.5.0-b42:compile",
        "javax.inject:javax.inject:jar:1:compile",
        "org.glassfish.hk2:hk2-utils:jar:2.5.0-b42:compile",
        "org.glassfish.hk2.external:aopalliance-repackaged:jar:2.5.0-b42:compile",
        "org.glassfish.jersey.core:jersey-client:jar:2.26:compile",
        "com.google.code.gson:gson:jar:2.8.6:compile",
        "ch.qos.logback:logback-core:jar:1.2.3:compile",
        "javax.servlet:javax.servlet-api:jar:4.0.1:compile",
        "org.eclipse.jetty:jetty-http:jar:9.4.14.v20181114:compile",
        "org.eclipse.jetty:jetty-io:jar:9.4.14.v20181114:compile",
        "com.datadoghq:java-dogstatsd-client:jar:3.0.1:compile",
        "com.github.jnr:jnr-unixsocket:jar:0.28:compile",
        "com.github.jnr:jnr-ffi:jar:2.1.12:compile",
        "com.github.jnr:jffi:jar:1.2.23:compile",
        "com.github.jnr:jffi:jar:native:1.2.23:runtime",
        "org.ow2.asm:asm:jar:7.1:compile",
        "org.ow2.asm:asm-commons:jar:7.1:compile",
        "org.ow2.asm:asm-analysis:jar:7.1:compile",
        "org.ow2.asm:asm-tree:jar:7.1:compile",
        "org.ow2.asm:asm-util:jar:7.1:compile",
        "com.github.jnr:jnr-a64asm:jar:1.0.0:compile",
        "com.github.jnr:jnr-x86asm:jar:1.0.2:compile",
        "com.github.jnr:jnr-constants:jar:0.9.15:compile",
        "com.github.jnr:jnr-enxio:jar:0.25:compile",
        "com.github.jnr:jnr-posix:jar:3.0.54:compile",
        "com.fsmatic:logs-backend-search:test-jar:tests:1.0-SNAPSHOT:test",
        "com.fsmatic:event-platform-usage-tracker-client:test-jar:tests:1.0-SNAPSHOT:test",
        "com.fsmatic:logs-backend-registry:test-jar:tests:1.0-SNAPSHOT:test",
        "com.fsmatic:logs-backend-indexing-service-client:test-jar:tests:1.0-SNAPSHOT:test",
        "nl.jqno.equalsverifier:equalsverifier:jar:3.3:test",
        "org.assertj:assertj-core:jar:3.16.1:test",
        "org.apache.kafka:kafka_2.12:jar:3.1.0:test",
        "org.apache.kafka:kafka-server-common:jar:3.1.0:test",
        "org.apache.kafka:kafka-metadata:jar:3.1.0:test",
        "org.apache.kafka:kafka-raft:jar:3.1.0:test",
        "org.apache.kafka:kafka-storage:jar:3.1.0:test",
        "org.apache.kafka:kafka-storage-api:jar:3.1.0:test",
        "net.sourceforge.argparse4j:argparse4j:jar:0.7.0:test",
        "com.fasterxml.jackson.module:jackson-module-scala_2.12:jar:2.12.3:test",
        "com.thoughtworks.paranamer:paranamer:jar:2.8:test",
        "com.fasterxml.jackson.dataformat:jackson-dataformat-csv:jar:2.12.3:test",
        "net.sf.jopt-simple:jopt-simple:jar:5.0.4:test",
        "org.bitbucket.b_c:jose4j:jar:0.7.8:compile",
        "com.yammer.metrics:metrics-core:jar:2.2.0:test",
        "org.scala-lang.modules:scala-collection-compat_2.12:jar:2.4.4:test",
        "org.scala-lang:scala-reflect:jar:2.12.10:test",
        "com.typesafe.scala-logging:scala-logging_2.12:jar:3.9.3:test",
        "commons-cli:commons-cli:jar:1.4:test",
        "org.openjdk.jmh:jmh-core:jar:1.20:test",
        "org.apache.commons:commons-math3:jar:3.6.1:test",
        "com.fsmatic:logs-backend-track-type-store:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-track-type-store:test-jar:tests:1.0-SNAPSHOT:test",
        "org.apache.logging.log4j:log4j-core:jar:2.17.2:test",
        "org.apache.logging.log4j:log4j-api:jar:2.17.2:test",
        "org.mongodb:bson:jar:4.2.0:compile",
        "com.google.protobuf:protobuf-java:jar:3.19.2:compile",
        "org.apache.kafka:kafka-clients:jar:3.1.0:compile",
        "org.xerial.snappy:snappy-java:jar:1.1.8.4:runtime",
        "com.fsmatic:logs-backend-context-loader-blob-common:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-dd-metrics:jar:1.0-SNAPSHOT:compile",
        "io.dropwizard.metrics:metrics-json:jar:3.2.2:compile",
        "io.dropwizard.metrics:metrics-jvm:jar:3.2.2:compile",
        "org.hdrhistogram:HdrHistogram:jar:2.1.12:compile",
        "com.fsmatic:logs-backend-dd-metrics:test-jar:tests:1.0-SNAPSHOT:test",
        "javax.ws.rs:javax.ws.rs-api:jar:2.1:compile",
        "com.squareup.okhttp3:okhttp:jar:3.14.9:compile",
        "com.fsmatic:logs-backend-rum-commons-domain:jar:1.0-SNAPSHOT:compile",
        "org.opentest4j:opentest4j:jar:1.2.0:test",
        "io.grpc:grpc-stub:jar:1.45.1:compile",
        "com.fsmatic:logs-backend-commons:jar:1.0-SNAPSHOT:compile",
        "io.opentracing:opentracing-noop:jar:0.32.0:compile",
        "com.fsmatic:logs-backend-registry-grpc:jar:1.0-SNAPSHOT:compile",
        "org.eclipse.jetty:jetty-util:jar:9.4.14.v20181114:compile",
        "com.google.guava:guava:jar:26.0-jre:compile",
        "com.google.j2objc:j2objc-annotations:jar:1.3:compile",
        "org.codehaus.mojo:animal-sniffer-annotations:jar:1.18:compile",
        "com.fsmatic:logs-backend-tracing-utils:jar:1.0-SNAPSHOT:compile",
        "com.datadoghq:dd-trace-ot:jar:0.101.0:compile",
        "io.opentracing.contrib:opentracing-tracerresolver:jar:0.1.0:compile",
        "com.squareup.moshi:moshi:jar:1.9.2:compile",
        "org.jctools:jctools-core:jar:3.3.0:compile",
        "com.datadoghq:sketches-java:jar:0.8.2:compile",
        "com.fsmatic:logs-backend-indexing-service-grpc:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:event-platform-track_types:jar:1.0-SNAPSHOT:compile",
        "com.google.errorprone:error_prone_annotations:jar:2.13.1:compile",
        "com.fsmatic:logs-backend-percolation:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-queryparser:jar:1.0-SNAPSHOT:compile",
        "org.antlr:antlr4-runtime:jar:4.7.1:compile",
        "com.ibm.icu:icu4j:jar:62.1:compile",
        "io.github.pengw0048:glob:jar:0.9.0:compile",
        "io.opentracing:opentracing-api:jar:0.32.0:compile",
        "com.github.ben-manes.caffeine:caffeine:jar:2.8.5:compile",
        "com.fasterxml.jackson.core:jackson-core:jar:2.12.0:compile",
        "com.fsmatic:logs-backend-org-routing:jar:1.0-SNAPSHOT:compile",
        "io.grpc:grpc-api:jar:1.45.1:compile",
        "com.typesafe:config:jar:1.3.1:compile",
        "com.fasterxml.jackson.core:jackson-annotations:jar:2.12.0:compile",
        "com.typesafe.akka:akka-actor_2.12:jar:2.5.32:compile",
        "org.scala-lang:scala-library:jar:2.12.8:compile",
        "com.fsmatic:logs-backend-notification-grpc:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-service:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-storage-cells-model:jar:1.0-SNAPSHOT:compile",
        "net.jodah:failsafe:jar:2.4.0:compile",
        "com.fasterxml.jackson.core:jackson-databind:jar:2.12.0:compile",
        "com.carrotsearch:hppc:jar:0.7.2:compile",
        "io.opentracing:opentracing-util:jar:0.32.0:compile",
        "com.fsmatic:logs-backend-mongo:jar:1.0-SNAPSHOT:compile",
        "io.grpc:grpc-core:jar:1.45.1:compile",
        "com.google.android:annotations:jar:4.1.1.4:runtime",
        "io.perfmark:perfmark-api:jar:0.23.0:runtime",
        "org.mongodb:mongodb-driver-core:jar:4.2.0:compile",
        "com.fsmatic:event-platform-tracks:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-assigner-utils:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-assigner-grpc:jar:1.0-SNAPSHOT:compile",
        "com.fsmatic:logs-backend-assigner-commons:jar:1.0-SNAPSHOT:compile",
        "com.google.code.findbugs:jsr305:jar:3.0.2:compile",
        "com.datadoghq:dd-java-agent:jar:0.101.0:provided",
        "org.immutables:value-annotations:jar:2.8.0:provided",
        "org.junit.jupiter:junit-jupiter-api:jar:5.6.2:test",
        "org.apiguardian:apiguardian-api:jar:1.1.2:test",
        "org.junit.platform:junit-platform-commons:jar:1.6.2:test",
        "org.junit.jupiter:junit-jupiter-engine:jar:5.6.2:test",
        "org.junit.platform:junit-platform-engine:jar:1.6.2:test",
        "org.junit.jupiter:junit-jupiter-params:jar:5.6.2:test",
        "net.jqwik:jqwik:jar:1.6.2:test",
        "net.jqwik:jqwik-api:jar:1.6.2:test",
        "net.jqwik:jqwik-web:jar:1.6.2:test",
        "net.jqwik:jqwik-time:jar:1.6.2:test",
        "net.jqwik:jqwik-engine:jar:1.6.2:test",
        "net.logstash.logback:logstash-logback-encoder:jar:4.11:compile"
      };
}
