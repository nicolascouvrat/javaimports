package com.nikodoko.javaimports.stdlib.internal;

import static com.google.common.io.Files.getNameWithoutExtension;

import com.google.common.base.MoreObjects;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GenerateStdlib {
  private static final Path apiPath = Paths.get("api");
  private static final ClassLoader loader = GenerateStdlib.class.getClassLoader();
  private static final Pattern apiPattern = Pattern.compile("pkg");

  private static class Importable {
    String pkg = "";
    String name = "";
    boolean isStatic;

    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("pkg", pkg)
          .add("name", name)
          .add("isStatic", isStatic)
          .toString();
    }
  }

  private static class JavaApi {
    String version;
    List<Importable> importables = new ArrayList<>();

    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("version", version)
          .add("importables", importables)
          .toString();
    }
  }

  public static void main(String[] args) {
    try {
      List<JavaApi> apis = loadApis();
      System.out.println(apis);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static List<JavaApi> loadApis() throws IOException {
    List<JavaApi> apis = new ArrayList<>();
    for (ResourceInfo resourceInfo : ClassPath.from(loader).getResources()) {
      Path resourcePath = Paths.get(resourceInfo.getResourceName());
      if (resourcePath.startsWith(apiPath)) {
        apis.add(loadApi(resourceInfo.getResourceName()));
      }
    }

    return apis;
  }

  private static JavaApi loadApi(String path) throws IOException {
    JavaApi api = new JavaApi();
    api.version = getVersion(Paths.get(path).getFileName().toString());
    try (InputStream stream = loader.getResourceAsStream(path)) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      api.importables = loadImportables(reader);
    }

    return api;
  }

  // Api files should be java-x.y.z.txt
  private static String getVersion(String apiName) {
    String[] fragments = getNameWithoutExtension(apiName).split("-");
    return fragments[1];
  }

  private static List<Importable> loadImportables(BufferedReader reader) throws IOException {
    List<Importable> importables = new ArrayList<>();
    String line;
    while ((line = reader.readLine()) != null) {
      importables.add(loadImportable(line));
    }

    return importables;
  }

  private static Importable loadImportable(String raw) {
    System.out.println(raw);
    return new Importable();
  }
}
