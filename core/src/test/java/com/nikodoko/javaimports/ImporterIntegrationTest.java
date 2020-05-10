package com.nikodoko.javaimports;

import static com.google.common.io.Files.getFileExtension;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;

import com.google.common.io.CharStreams;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ImporterIntegrationTest {
  private static final Path dataPath = Paths.get("com/nikodoko/javaimports/testdata");
  private static final String outputExtension = "output";
  private static final String inputExtension = "input";

  @Parameters(name = "{index}: {0}")
  public static Iterable<Object[]> data() throws Exception {
    ClassLoader classLoader = ImporterIntegrationTest.class.getClassLoader();

    Map<String, Object[]> inputs = new HashMap<>();
    Map<String, Object[]> outputs = new HashMap<>();
    for (ResourceInfo resourceInfo : ClassPath.from(classLoader).getResources()) {
      String resourceName = resourceInfo.getResourceName();
      Path resourcePath = Paths.get(resourceName);
      if (resourcePath.startsWith(dataPath)) {
        Path relPath = dataPath.relativize(resourcePath);
        assertWithMessage("bad testdata").that(relPath.getNameCount()).isEqualTo(2);
        String extension = getFileExtension(relPath.getFileName().toString());
        String pkgName = relPath.subpath(0, 1).toString();
        if (!extension.equals(outputExtension) && !extension.equals(inputExtension)) {
          // It's another file in the input package, ignore it
          continue;
        }

        String contents;
        try (InputStream stream = classLoader.getResourceAsStream(resourceName)) {
          contents = CharStreams.toString(new InputStreamReader(stream, UTF_8));
        }

        // Make sure we get the absolute path of the resource
        Object[] data = {Paths.get(resourceInfo.url().toURI()), contents};
        if (extension.equals(outputExtension)) {
          outputs.put(pkgName, data);
          continue;
        }

        inputs.put(pkgName, data);
      }
    }

    for (String k : inputs.keySet()) {
      assertWithMessage("unmatched inputs and outputs").that(outputs).containsKey(k);
    }

    List<Object[]> testData = new ArrayList<>();
    for (Map.Entry<String, Object[]> entry : inputs.entrySet()) {
      testData.add(
          new Object[] {
            entry.getKey(), // pkgName
            entry.getValue()[0], // filepath
            entry.getValue()[1], // contents
            outputs.get(entry.getKey())[1] // output contents
          });
    }

    return testData;
  }

  private final String pkgName;
  private final Path filepath;
  private final String input;
  private final String expected;

  public ImporterIntegrationTest(String pkgName, Path filepath, String input, String expected) {
    this.pkgName = pkgName;
    this.filepath = filepath;
    this.input = input;
    this.expected = expected;
  }

  @Test
  public void testAddUsedImports() {
    try {
      String output = new Importer().addUsedImports(filepath, input);
      assertWithMessage("bad output for " + filepath).that(output).isEqualTo(expected);
    } catch (ImporterException e) {
      for (ImporterException.ImporterDiagnostic d : e.diagnostics()) {
        System.out.println(d);
      }
      fail();
    }
  }
}
