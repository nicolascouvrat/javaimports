package com.nikodoko.javaimports.environment.bazel;

import static com.google.common.truth.Truth.assertThat;
import static java.io.FileDescriptor.in;

import com.nikodoko.javaimports.environment.shared.Dependency;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BazelQueryResultsTest {
  @Test
  public void itShouldParseRawResults() throws Exception {
    var raw =
        """
1 //collect:collect
2 //collect:src/main/java/collect/AbstractBiMap.java
2 //collect:src/main/java/collect/BaseImmutableMultimap.java
2 //collect:src/main/java/collect/CartesianList.java
2 //collect:src/main/java/collect/DenseImmutableTable.java
2 //collect:src/main/java/collect/EmptyContiguousSet.java
2 //collect:src/main/java/collect/FilteredEntryMultimap.java
2 //collect:src/main/java/collect/GeneralRange.java
2 //collect:src/main/java/collect/HashBasedTable.java
2 //collect:src/main/java/collect/ImmutableAsList.java
2 //collect:src/main/java/collect/JdkBackedImmutableBiMap.java
2 //collect:src/main/java/collect/LexicographicalOrdering.java
2 //collect:src/main/java/collect/MapDifference.java
2 //collect:src/main/java/collect/NaturalOrdering.java
2 //collect:src/main/java/collect/package/info.java
3 @bazel_tools//src/conditions:host_windows
3 @bazel_tools//src/main/cpp/util:errors_posix.cc
3 @@bazel_tools~cc_configure_extension~local_config_cc//:builtin_include_directory_paths
3 @platforms//os:os
3 @platforms//os:windows
3 @@rules_java~//toolchains:current_java_toolchain
3 @rules_jvm_external//private/tools/java/com/github/bazelbuild/rules_jvm_external:ByteStreams.java
3 @rules_jvm_external//private/tools/java/com/github/bazelbuild/rules_jvm_external:Coordinates.java
3 @rules_jvm_external//private/tools/java/com/github/bazelbuild/rules_jvm_external:Hasher.java
3 @rules_jvm_external//private/tools/java/com/github/bazelbuild/rules_jvm_external:rules_jvm_external
3 @rules_jvm_external//private/tools/java/com/github/bazelbuild/rules_jvm_external/jar:AddJarManifestEntry
3 @rules_jvm_external//private/tools/java/com/github/bazelbuild/rules_jvm_external/jar:AddJarManifestEntry.java
3 @rules_jvm_external//settings:stamp_manifest
4 @maven//:com_mycompany_app_a_dependency
4 @maven//:com_mycompany_app_an_empty_dependency
4 @maven//:com_mycompany_app_an_indirect_dependency
4 @maven//:com_mycompany_app_another_dependency
4 @maven//:v1/com/mycompany/app/a-dependency/1.0/a-dependency-1.0.jar
4 @maven//:v1/com/mycompany/app/an-empty-dependency/1.0/an-empty-dependency-1.0.jar
4 @maven//:v1/com/mycompany/app/an-indirect-dependency/1.0/an-indirect-dependency-1.0.jar
4 @maven//:v1/com/mycompany/app/another-dependency/1.0/another-dependency-1.0.jar
""";
    var expected =
        List.of(
            direct("/Users/nicolas.couvrat/root/collect/src/main/java/collect/AbstractBiMap.java"),
            direct(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/BaseImmutableMultimap.java"),
            direct("/Users/nicolas.couvrat/root/collect/src/main/java/collect/CartesianList.java"),
            direct(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/DenseImmutableTable.java"),
            direct(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/EmptyContiguousSet.java"),
            direct(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/FilteredEntryMultimap.java"),
            direct("/Users/nicolas.couvrat/root/collect/src/main/java/collect/GeneralRange.java"),
            direct("/Users/nicolas.couvrat/root/collect/src/main/java/collect/HashBasedTable.java"),
            direct(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/ImmutableAsList.java"),
            direct(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/JdkBackedImmutableBiMap.java"),
            direct(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/LexicographicalOrdering.java"),
            direct("/Users/nicolas.couvrat/root/collect/src/main/java/collect/MapDifference.java"),
            direct(
                "/Users/nicolas.couvrat/root/collect/src/main/java/collect/NaturalOrdering.java"),
            direct("/Users/nicolas.couvrat/root/collect/src/main/java/collect/package/info.java"));

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
1 @maven//:com_mycompany_app_a_dependency
1 @maven//:com_mycompany_app_an_empty_dependency
3 @maven//:com_mycompany_app_an_indirect_dependency
3 @maven//:com_mycompany_app_another_dependency
1 @maven//:v1/com/mycompany/app/a-dependency/1.0/a-dependency-1.0.jar
1 @maven//:v1/com/mycompany/app/an-empty-dependency/1.0/an-empty-dependency-1.0.jar
3 @maven//:v1/com/mycompany/app/an-indirect-dependency/1.0/an-indirect-dependency-1.0.jar
3 @maven//:v1/com/mycompany/app/another-dependency/1.0/another-dependency-1.0.jar
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
            direct(
                "/output/base/external/rules_jvm_external~~maven~maven/v1/com/mycompany/app/a-dependency/1.0/a-dependency-1.0.jar"),
            direct(
                "/output/base/external/rules_jvm_external~~maven~maven/v1/com/mycompany/app/an-empty-dependency/1.0/an-empty-dependency-1.0.jar"),
            transitive(
                "/output/base/external/rules_jvm_external~~maven~maven/v1/com/mycompany/app/an-indirect-dependency/1.0/an-indirect-dependency-1.0.jar"),
            transitive(
                "/output/base/external/rules_jvm_external~~maven~maven/v1/com/mycompany/app/another-dependency/1.0/another-dependency-1.0.jar"));
  }

  @Test
  void itShouldParseDependenciesForWorkspaceWithPin() throws Exception {
    var raw =
        """
1 @com_google_guava_failureaccess_1_0_2//file:file
1 @com_google_guava_failureaccess_1_0_2//file:v1/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar
2 @com_google_guava_guava_33_2_0_jre//file:file
2 @com_google_guava_guava_33_2_0_jre//file:v1/com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar
2 @maven//:com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar
1 @maven//:com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar
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
            direct(
                "/output/base/external/com_google_guava_guava_33_2_0_jre/file/v1/com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar"),
            direct(
                "/output/base/external/com_google_guava_failureaccess_1_0_2/file/v1/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar"));
  }

  @Test
  void itShouldParseDependenciesForWorkspaceNoPin() throws Exception {
    var raw =
        """
1 @maven//:com_google_guava_failureaccess
3 @maven//:com_google_guava_guava
1 @maven//:v1/https/repo1.maven.org/maven2/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar
3 @maven//:v1/https/repo1.maven.org/maven2/com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar
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
            transitive(
                "/output/base/external/maven/v1/https/repo1.maven.org/maven2/com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar"),
            direct(
                "/output/base/external/maven/v1/https/repo1.maven.org/maven2/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar"));
  }

  @Test
  void itShouldParseDependenciesForModuleWithPin() throws Exception {
    var raw =
        """
1 @@rules_jvm_external~~maven~com_google_guava_failureaccess_1_0_2//file:file
1 @@rules_jvm_external~~maven~com_google_guava_failureaccess_1_0_2//file:v1/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar
3 @@rules_jvm_external~~maven~com_google_guava_guava_33_2_0_jre//file:file
3 @@rules_jvm_external~~maven~com_google_guava_guava_33_2_0_jre//file:v1/com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar
1 @maven//:com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar
3 @maven//:com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar
1 @maven//:com_google_guava_failureaccess
1 @maven//:com_google_guava_failureaccess_1_0_2_extension
3 @maven//:com_google_guava_guava
3 @maven//:com_google_guava_guava_33_2_0_jre_extension
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
            transitive(
                "/output/base/external/rules_jvm_external~~maven~com_google_guava_guava_33_2_0_jre/file/v1/com/google/guava/guava/33.2.0-jre/guava-33.2.0-jre.jar"),
            direct(
                "/output/base/external/rules_jvm_external~~maven~com_google_guava_failureaccess_1_0_2/file/v1/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar"));
  }

  public static BazelDependency direct(String path) {
    return new BazelDependency(Dependency.Kind.DIRECT, Paths.get(path));
  }

  public static BazelDependency transitive(String path) {
    return new BazelDependency(Dependency.Kind.TRANSITIVE, Paths.get(path));
  }
}
