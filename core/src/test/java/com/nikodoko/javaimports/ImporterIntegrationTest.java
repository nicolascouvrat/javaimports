package com.nikodoko.javaimports;

import static com.google.common.io.Files.getFileExtension;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.nikodoko.javaimports.common.CommonTestUtil.aStaticImport;
import static com.nikodoko.javaimports.common.CommonTestUtil.anImport;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;

import com.google.common.io.CharStreams;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.stdlib.BasicStdlibProvider;
import com.nikodoko.javaimports.stdlib.FakeStdlib;
import com.nikodoko.packagetest.BuildSystem;
import com.nikodoko.packagetest.Export;
import com.nikodoko.packagetest.Exported;
import com.nikodoko.packagetest.Module;
import com.nikodoko.packagetest.Repository;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ImporterIntegrationTest {
  private static final URL repositoryURL =
      ImporterIntegrationTest.class.getResource("/.m2/repository");
  private static final Path dataPath = Paths.get("com/nikodoko/javaimports/testdata");

  static class TestPkg {
    List<Module.File> files = new ArrayList<>();
    String fileToFix = "";
    String expected = "";
    String name = "";

    TestPkg(String name) {
      this.name = name;
    }
  }

  Exported testPkg;

  @BeforeEach
  void setup() {
    Logs.enable();
  }

  @AfterEach
  void cleanup() throws IOException {
    testPkg.cleanup();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testPackageProvider")
  void testAddUsedImportsBzl(String name, TestPkg pkg) throws Exception {
    testPkg = setup(pkg, BuildSystem.BAZEL);
    Path main = testPkg.file(pkg.name, pkg.fileToFix).get();
    Options opts =
        Options.builder()
            .debug(true)
            .repository(Paths.get(repositoryURL.toURI()))
            .stdlib(
                new BasicStdlibProvider(
                    new FakeStdlib(
                        aStaticImport("java.fakeutil.anotherPublicField"),
                        aStaticImport("java.fakeutil.aSubclassPublicStaticField"),
                        aStaticImport("java.fakeutil.valueOf"),
                        aStaticImport("java.fakeutil.toString"),
                        aStaticImport("java.fakeutil.add"),
                        aStaticImport("java.fakeutil.T"),
                        anImport("java.fakeutil.AnotherProtectedClass"),
                        anImport("java.fakeutil.ALocalPublicClass"),
                        anImport("java.fakeutil.App"),
                        anImport("java.util.ArrayList"),
                        anImport("java.util.AbstractList"),
                        anImport("java.lang.Enum"),
                        anImport("java.util.List"))))
            // speed up tests a bit
            .numThreads(Runtime.getRuntime().availableProcessors())
            .build();
    String input = new String(Files.readAllBytes(main), UTF_8);
    try {
      // if (!pkg.name.equals("withotherpackage")) {
      //   return;
      // }
      String output = new Importer(opts).addUsedImports(main, input);
      assertWithMessage("bad output for " + pkg.name).that(output).isEqualTo(pkg.expected);
    } catch (ImporterException e) {
      for (ImporterException.ImporterDiagnostic d : e.diagnostics()) {
        System.out.println(d);
      }
      fail();
    } catch (Exception e) {
      throw new AssertionError("Got exception for " + pkg.name, e);
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testPackageProvider")
  void testAddUsedImports(String name, TestPkg pkg) throws Exception {
    testPkg = setup(pkg, BuildSystem.MAVEN);
    Path main = testPkg.file(pkg.name, pkg.fileToFix).get();
    Options opts =
        Options.builder()
            .debug(true)
            .repository(Paths.get(repositoryURL.toURI()))
            .stdlib(
                new BasicStdlibProvider(
                    new FakeStdlib(
                        aStaticImport("java.fakeutil.anotherPublicField"),
                        aStaticImport("java.fakeutil.aSubclassPublicStaticField"),
                        aStaticImport("java.fakeutil.valueOf"),
                        aStaticImport("java.fakeutil.add"),
                        aStaticImport("java.fakeutil.T"),
                        anImport("java.fakeutil.AnotherProtectedClass"),
                        anImport("java.fakeutil.ALocalPublicClass"),
                        anImport("java.fakeutil.App"),
                        anImport("java.util.ArrayList"),
                        anImport("java.util.AbstractList"),
                        anImport("java.lang.Enum"),
                        anImport("java.util.List"))))
            // speed up tests a bit
            .numThreads(Runtime.getRuntime().availableProcessors())
            .build();
    String input = new String(Files.readAllBytes(main), UTF_8);
    try {
      String output = new Importer(opts).addUsedImports(main, input);
      assertWithMessage("bad output for " + pkg.name).that(output).isEqualTo(pkg.expected);
    } catch (ImporterException e) {
      for (ImporterException.ImporterDiagnostic d : e.diagnostics()) {
        System.out.println(d);
      }
      fail();
    } catch (Exception e) {
      throw new AssertionError("Got exception for " + pkg.name, e);
    }
  }

  Exported setup(TestPkg pkg, BuildSystem system) throws IOException, URISyntaxException {
    var localRepo = Paths.get(repositoryURL.toURI());
    var localRepoUrl = "file://%s".formatted(localRepo);
    Repository repoLocal = Repository.named("local").at(localRepoUrl);
    Repository repoCentral = Repository.named("central").at("https://repo1.maven.org/maven2");
    Module module =
        Module.named(pkg.name)
            .containing(pkg.files.toArray(new Module.File[pkg.files.size()]))
            .dependingOn(
                Module.dependency("com.mycompany.app", "a-dependency", "1.0"),
                Module.dependency("com.mycompany.app", "another-dependency", "1.0"),
                Module.dependency("com.mycompany.app", "an-empty-dependency", "1.0"));
    return Export.of(system, List.of(repoLocal, repoCentral), List.of(module));
  }

  static Stream<Arguments> testPackageProvider() throws IOException {
    ClassLoader classLoader = ImporterIntegrationTest.class.getClassLoader();
    Map<String, TestPkg> packages = new HashMap<>();
    for (ResourceInfo resourceInfo : ClassPath.from(classLoader).getResources()) {
      Path absolutePath = Paths.get(resourceInfo.getResourceName());
      if (absolutePath.startsWith(dataPath)) {
        Path relativePath = dataPath.relativize(absolutePath);

        String pkgName = relativePath.subpath(0, 1).toString();
        String extension = getFileExtension(relativePath.getFileName().toString());
        String pathFragment = toPathFragment(relativePath);
        String contents;
        try (InputStream stream = classLoader.getResourceAsStream(resourceInfo.getResourceName())) {
          contents = CharStreams.toString(new InputStreamReader(stream, UTF_8));
        }

        TestPkg pkg = packages.computeIfAbsent(pkgName, p -> new TestPkg(p));
        switch (extension) {
          case "output":
            pkg.expected = contents;
            break;
          case "input":
            pkg.fileToFix = pathFragment;
            pkg.files.add(Module.file(pathFragment, contents));
            break;
          default:
            pkg.files.add(Module.file(pathFragment, contents));
            break;
        }
      }
    }

    return packages.values().stream().map(pkg -> Arguments.of(pkg.name, pkg));
  }

  static String toPathFragment(Path resourceRelativePath) {
    // In order to support multiple folders, authorize the following naming pattern: a-B.java ->
    // a/B.java
    String withJavaExtension = resourceRelativePath.getFileName().toString() + ".java";
    return withJavaExtension.replace("-", "/");
  }
}
