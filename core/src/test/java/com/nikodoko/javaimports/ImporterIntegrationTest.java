package com.nikodoko.javaimports;

import static com.google.common.io.Files.getFileExtension;
import static com.google.common.io.Files.getNameWithoutExtension;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;

import com.google.common.io.CharStreams;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;
import com.nikodoko.packagetest.BuildSystem;
import com.nikodoko.packagetest.Export;
import com.nikodoko.packagetest.Exported;
import com.nikodoko.packagetest.Module;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ImporterIntegrationTest {
  private static final URL repositoryURL =
      ImporterIntegrationTest.class.getResource("/testrepository");
  private static final Path dataPath = Paths.get("com/nikodoko/javaimports/testdata");
  private static final String outputExtension = "output";
  private static final String inputExtension = "input";

  @Parameters(name = "{index}: {0}")
  public static Iterable<Object[]> data() throws Exception {
    class PkgInfo {
      public List<Module.File> files = new ArrayList<>();
      public String target = "";
    }

    ClassLoader classLoader = ImporterIntegrationTest.class.getClassLoader();

    Map<String, PkgInfo> inputs = new HashMap<>();
    Map<String, String> outputs = new HashMap<>();
    for (ResourceInfo resourceInfo : ClassPath.from(classLoader).getResources()) {
      String resourceName = resourceInfo.getResourceName();
      Path resourcePath = Paths.get(resourceName);
      if (resourcePath.startsWith(dataPath)) {
        Path relPath = dataPath.relativize(resourcePath);
        assertWithMessage("bad testdata").that(relPath.getNameCount()).isEqualTo(2);

        String extension = getFileExtension(relPath.getFileName().toString());
        String fileName = getNameWithoutExtension(relPath.getFileName().toString()) + ".java";
        String pkgName = relPath.subpath(0, 1).toString();
        String contents;
        try (InputStream stream = classLoader.getResourceAsStream(resourceName)) {
          contents = CharStreams.toString(new InputStreamReader(stream, UTF_8));
        }

        if (extension.equals(outputExtension)) {
          outputs.put(pkgName, contents);
          continue;
        }

        PkgInfo pkg = inputs.get(pkgName);
        if (pkg == null) {
          pkg = new PkgInfo();
          inputs.put(pkgName, pkg);
        }

        // In order to support multiple folders, authorize the following naming pattern: a-B.java ->
        // a/B.java
        pkg.files.add(Module.file(fileName.replace("-", "/"), contents));
        if (extension.equals(inputExtension)) {
          pkg.target = fileName;
        }
      }
    }

    for (String k : inputs.keySet()) {
      assertWithMessage("unmatched inputs and outputs").that(outputs).containsKey(k);
      assertWithMessage("package without target: " + k).that(inputs.get(k).target).isNotEmpty();
    }

    List<Object[]> testData = new ArrayList<>();
    for (Map.Entry<String, PkgInfo> entry : inputs.entrySet()) {
      Module.File[] moduleFiles = new Module.File[entry.getValue().files.size()];
      testData.add(
          new Object[] {
            entry.getValue().target, // target name
            Module.named(entry.getKey())
                .containing(entry.getValue().files.toArray(moduleFiles))
                .dependingOn(
                    Module.dependency("com.mycompany.app", "a-dependency", "1.0")), // test module
            outputs.get(entry.getKey()) // output contents
          });
    }

    return testData;
  }

  private final Module module;
  private final String target;
  private final String expected;
  private Exported testPkg;

  public ImporterIntegrationTest(String target, Module module, String expected) {
    this.module = module;
    this.target = target;
    this.expected = expected;
  }

  @Before
  public void setup() throws Exception {
    this.testPkg = Export.of(BuildSystem.MAVEN, module);
  }

  @After
  public void cleanup() throws Exception {
    this.testPkg.cleanup();
  }

  @Test
  public void testAddUsedImports() {
    Path main = testPkg.file(module.name(), target).get();
    try {
      String input = new String(Files.readAllBytes(main), UTF_8);
      Options opts =
          Options.builder().debug(false).repository(Paths.get(repositoryURL.toURI())).build();
      String output = new Importer(opts).addUsedImports(main, input);
      assertWithMessage("bad output for " + module.name()).that(output).isEqualTo(expected);
    } catch (ImporterException e) {
      for (ImporterException.ImporterDiagnostic d : e.diagnostics()) {
        System.out.println(d);
      }
      fail();
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }
}
