package com.nikodoko.javaimports;

import static com.google.common.truth.Truth.assertWithMessage;

import com.nikodoko.packagetest.BuildSystem;
import com.nikodoko.packagetest.Export;
import com.nikodoko.packagetest.Exported;
import com.nikodoko.packagetest.Module;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class NativeImageIT {
  private static final String INPUT = ".input";
  private static final String OUTPUT = ".output";
  private static final String JAVAIMPORTS_BINARY = "./javaimports-native-image";

  Exported targetPkg;

  @ParameterizedTest(name = "{0}")
  @MethodSource("packageProvider")
  void testNativeImage(String name, Pkg pkg) throws Exception {
    targetPkg = export(pkg);
    var targetFile = targetPkg.file(pkg.name, pkg.target).get();
    var process = runJavaimportsOn(targetFile);
    process.waitFor();

    var got = new String(process.getInputStream().readAllBytes());
    var errorMsg = new String(process.getErrorStream().readAllBytes());
    assertWithMessage(String.format("bad output for: %s, error message: %s", pkg.name, errorMsg))
        .that(got)
        .isEqualTo(pkg.expected);
  }

  Process runJavaimportsOn(Path target) throws Exception {
    var javaHome = System.getProperty("java.home");
    return new ProcessBuilder(
            JAVAIMPORTS_BINARY, String.format("-Djava.home=%s", javaHome), target.toString())
        .directory(Paths.get(getClass().getResource("/").toURI()).getParent().toFile())
        .start();
  }

  static Exported export(Pkg pkg) throws Exception {
    var module =
        Module.named(pkg.name).containing(pkg.files.toArray(new Module.File[pkg.files.size()]));
    return Export.of(BuildSystem.MAVEN, module);
  }

  static Stream<Arguments> packageProvider() throws Exception {
    return getFilesPerTestPackage().entrySet().stream()
        .map(e -> testPackage(e.getKey(), e.getValue()))
        .map(p -> Arguments.of(p.name, p));
  }

  static Pkg testPackage(String name, List<Path> files) {
    var pkg = new Pkg(name);
    for (var f : files) {
      var contents = readString(f);
      var pathFragment = getPathFragment(stripExtension(f));
      switch (getExtension(f)) {
        case OUTPUT:
          pkg.expected = contents;
          break;
        case INPUT:
          pkg.target = pathFragment;
          pkg.files.add(Module.file(pathFragment, contents));
          break;
        default:
          pkg.files.add(Module.file(pathFragment, contents));
          break;
      }
    }
    return pkg;
  }

  static String readString(Path p) {
    try {
      return Files.readString(p);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static Map<String, List<Path>> getFilesPerTestPackage() throws Exception {
    var fixtures = Paths.get(NativeImageIT.class.getClassLoader().getResource("fixtures").toURI());
    return Files.walk(fixtures)
        .map(Path::toFile)
        .filter(File::isFile)
        // To avoid failing tests because there's a .swp file left there...
        .filter(f -> !f.isHidden())
        .map(File::toPath)
        .collect(Collectors.groupingBy(f -> f.getParent().getFileName().toString()));
  }

  static String getPathFragment(Path path) {
    return path.getFileName().toString().replace("-", "/") + ".java";
  }

  static Path stripExtension(Path path) {
    var cutoff = path.toString().lastIndexOf(".");
    if (cutoff == -1) {
      return path;
    }

    return Paths.get(path.toString().substring(0, cutoff));
  }

  static String getExtension(Path path) {
    var cutoff = path.toString().lastIndexOf(".");
    if (cutoff == -1) {
      return "";
    }

    return path.toString().substring(cutoff);
  }

  static final class Pkg {
    String target = "";
    String expected = "";
    String name = "";
    List<Module.File> files = new ArrayList<>();

    Pkg(String name) {
      this.name = name;
    }
  }
}
