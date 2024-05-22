package com.nikodoko.javaimports.environment.bazel;

import static com.google.common.truth.Truth.assertThat;
import static java.io.FileDescriptor.in;

import java.io.StringReader;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BazelQueryResultsTest {
  @Test
  public void itShouldParseRawResults() throws Exception {
    var raw =
        """
//collect:collect
//collect:src/main/java/collect/AbstractBiMap.java
//collect:src/main/java/collect/BaseImmutableMultimap.java
//collect:src/main/java/collect/CartesianList.java
//collect:src/main/java/collect/DenseImmutableTable.java
//collect:src/main/java/collect/EmptyContiguousSet.java
//collect:src/main/java/collect/FilteredEntryMultimap.java
//collect:src/main/java/collect/GeneralRange.java
//collect:src/main/java/collect/HashBasedTable.java
//collect:src/main/java/collect/ImmutableAsList.java
//collect:src/main/java/collect/JdkBackedImmutableBiMap.java
//collect:src/main/java/collect/LexicographicalOrdering.java
//collect:src/main/java/collect/MapDifference.java
//collect:src/main/java/collect/NaturalOrdering.java
//collect:src/main/java/collect/package/info.java
@bazel_tools//src/conditions:host_windows
@bazel_tools//src/main/cpp/util:errors_posix.cc
@@bazel_tools~cc_configure_extension~local_config_cc//:builtin_include_directory_paths
@platforms//os:os
@platforms//os:windows
@@rules_java~//toolchains:current_java_toolchain
@rules_jvm_external//private/tools/java/com/github/bazelbuild/rules_jvm_external:ByteStreams.java
@rules_jvm_external//private/tools/java/com/github/bazelbuild/rules_jvm_external:Coordinates.java
@rules_jvm_external//private/tools/java/com/github/bazelbuild/rules_jvm_external:Hasher.java
@rules_jvm_external//private/tools/java/com/github/bazelbuild/rules_jvm_external:rules_jvm_external
@rules_jvm_external//private/tools/java/com/github/bazelbuild/rules_jvm_external/jar:AddJarManifestEntry
@rules_jvm_external//private/tools/java/com/github/bazelbuild/rules_jvm_external/jar:AddJarManifestEntry.java
@rules_jvm_external//settings:stamp_manifest
@maven//:com_mycompany_app_a_dependency
@maven//:com_mycompany_app_an_empty_dependency
@maven//:com_mycompany_app_an_indirect_dependency
@maven//:com_mycompany_app_another_dependency
@maven//:v1/com/mycompany/app/a-dependency/1.0/a-dependency-1.0.jar
@maven//:v1/com/mycompany/app/an-empty-dependency/1.0/an-empty-dependency-1.0.jar
@maven//:v1/com/mycompany/app/an-indirect-dependency/1.0/an-indirect-dependency-1.0.jar
@maven//:v1/com/mycompany/app/another-dependency/1.0/another-dependency-1.0.jar
""";
    var expected =
        List.of(
            Paths.get(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/AbstractBiMap.java"),
            Paths.get(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/BaseImmutableMultimap.java"),
            Paths.get(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/CartesianList.java"),
            Paths.get(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/DenseImmutableTable.java"),
            Paths.get(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/EmptyContiguousSet.java"),
            Paths.get(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/FilteredEntryMultimap.java"),
            Paths.get(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/GeneralRange.java"),
            Paths.get(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/HashBasedTable.java"),
            Paths.get(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/ImmutableAsList.java"),
            Paths.get(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/JdkBackedImmutableBiMap.java"),
            Paths.get(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/LexicographicalOrdering.java"),
            Paths.get(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/MapDifference.java"),
            Paths.get(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/NaturalOrdering.java"),
            Paths.get(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/package/info.java"));

    var input = new StringReader(raw);
    var got =
        BazelQueryResults.parser()
            .workspaceRoot(Paths.get("/Users/nicolas.couvrat/root"))
            .outputBase(Paths.get("/output/base"))
            .isModule(true)
            .parse(input);
    assertThat(got.srcs()).containsExactlyElementsIn(expected);
  }

  @Test
  void itShouldParseDependenciesForModuleNoPin() throws Exception {
    var raw =
        """
@maven//:com_mycompany_app_a_dependency
@maven//:com_mycompany_app_an_empty_dependency
@maven//:com_mycompany_app_an_indirect_dependency
@maven//:com_mycompany_app_another_dependency
@maven//:v1/com/mycompany/app/a-dependency/1.0/a-dependency-1.0.jar
@maven//:v1/com/mycompany/app/an-empty-dependency/1.0/an-empty-dependency-1.0.jar
@maven//:v1/com/mycompany/app/an-indirect-dependency/1.0/an-indirect-dependency-1.0.jar
@maven//:v1/com/mycompany/app/another-dependency/1.0/another-dependency-1.0.jar
""";

    var in = new StringReader(raw);
    var got =
        BazelQueryResults.parser()
            .workspaceRoot(Paths.get(""))
            .outputBase(Paths.get("/output/base"))
            .isModule(true)
            .parse(in);
    assertThat(got.deps())
        .containsExactly(
            Paths.get(
                "/output/base/external/rules_jvm_external~~maven~maven/v1/com/mycompany/app/a-dependency/1.0/a-dependency-1.0.jar"),
            Paths.get(
                "/output/base/external/rules_jvm_external~~maven~maven/v1/com/mycompany/app/an-empty-dependency/1.0/an-empty-dependency-1.0.jar"),
            Paths.get(
                "/output/base/external/rules_jvm_external~~maven~maven/v1/com/mycompany/app/an-indirect-dependency/1.0/an-indirect-dependency-1.0.jar"),
            Paths.get(
                "/output/base/external/rules_jvm_external~~maven~maven/v1/com/mycompany/app/another-dependency/1.0/another-dependency-1.0.jar"));
  }

  @Test
  void itShouldParseDependenciesForWorkspaceWithPin() throws Exception {
    var raw =
        """
@com_google_guava_failureaccess_1_0_2//file:file
@com_google_guava_failureaccess_1_0_2//file:v1/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar
@com_google_guava_guava_33_2_0_jre//file:file
@com_google_guava_guava_33_2_0_jre//file:v1/com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar
@maven//:com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar
@maven//:com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar
""";

    var in = new StringReader(raw);
    var got =
        BazelQueryResults.parser()
            .workspaceRoot(Paths.get(""))
            .outputBase(Paths.get("/output/base"))
            .isModule(false)
            .parse(in);
    assertThat(got.deps())
        .containsExactly(
            Paths.get(
                "/output/base/external/com_google_guava_guava_33_2_0_jre/file/v1/com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar"),
            Paths.get(
                "/output/base/external/com_google_guava_failureaccess_1_0_2/file/v1/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar"));
  }

  @Test
  void itShouldParseDependenciesForWorkspaceNoPin() throws Exception {
    var raw =
        """
@maven//:com_google_guava_failureaccess
@maven//:com_google_guava_guava
@maven//:v1/https/repo1.maven.org/maven2/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar
@maven//:v1/https/repo1.maven.org/maven2/com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar
""";

    var in = new StringReader(raw);
    var got =
        BazelQueryResults.parser()
            .workspaceRoot(Paths.get(""))
            .outputBase(Paths.get("/output/base"))
            .isModule(false)
            .parse(in);
    assertThat(got.deps())
        .containsExactly(
            Paths.get(
                "/output/base/external/maven/v1/https/repo1.maven.org/maven2/com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar"),
            Paths.get(
                "/output/base/external/maven/v1/https/repo1.maven.org/maven2/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar"));
  }

  @Test
  void itShouldParseDependenciesForModuleWithPin() throws Exception {
    var raw =
        """
@@rules_jvm_external~~maven~com_google_guava_failureaccess_1_0_2//file:file
@@rules_jvm_external~~maven~com_google_guava_failureaccess_1_0_2//file:v1/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar
@@rules_jvm_external~~maven~com_google_guava_guava_33_2_0_jre//file:file
@@rules_jvm_external~~maven~com_google_guava_guava_33_2_0_jre//file:v1/com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar
@maven//:com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar
@maven//:com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar
@maven//:com_google_guava_failureaccess
@maven//:com_google_guava_failureaccess_1_0_2_extension
@maven//:com_google_guava_guava
@maven//:com_google_guava_guava_33_2_0_jre_extension
""";

    var in = new StringReader(raw);
    var got =
        BazelQueryResults.parser()
            .workspaceRoot(Paths.get(""))
            .outputBase(Paths.get("/output/base"))
            .isModule(true)
            .parse(in);
    assertThat(got.deps())
        .containsExactly(
            Paths.get(
                "/output/base/external/rules_jvm_external~~maven~com_google_guava_guava_33_2_0_jre/file/v1/com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar"),
            Paths.get(
                "/output/base/external/rules_jvm_external~~maven~com_google_guava_failureaccess_1_0_2/file/v1/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar"));
  }
}
