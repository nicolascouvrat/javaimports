package com.nikodoko.javaimports.stdlib.internal;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.MoreObjects;
import com.google.common.io.Files;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;
import com.google.googlejavaformat.java.FormatterException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateStdlib {
  private static final Path apiPath = Paths.get("api");
  private static final ClassLoader loader = GenerateStdlib.class.getClassLoader();
  private static final Pattern apiFileNamePattern =
      Pattern.compile("java-(?<version>\\d+\\.\\d+)\\.txt");
  private static final Pattern importablePattern =
      Pattern.compile("pkg (?<pkg>\\S+) class (?<class>\\S+)(?:, static (?<identifier>\\w+))?");

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

    public void output(PrintWriter out) {
      out.printf("new Import(\"%s\",\"%s\",%b)", name, pkg, isStatic);
    }
  }

  private static class JavaApi {
    String version;
    Map<String, List<Importable>> importables = new HashMap<>();

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
      for (JavaApi api : apis) {
        output(api);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static List<JavaApi> loadApis() throws IOException {
    List<JavaApi> apis = new ArrayList<>();
    for (ResourceInfo resourceInfo : ClassPath.from(loader).getResources()) {
      Path resourcePath = Paths.get(resourceInfo.getResourceName());
      Matcher m = apiFileNamePattern.matcher(resourcePath.getFileName().toString());
      if (m.matches()) {
        apis.add(loadApi(m.group("version"), resourceInfo.getResourceName()));
      }
    }

    return apis;
  }

  private static JavaApi loadApi(String version, String path) throws IOException {
    JavaApi api = new JavaApi();
    api.version = version;
    try (InputStream stream = loader.getResourceAsStream(path)) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      api.importables = loadImportables(reader);
    }

    return api;
  }

  private static Map<String, List<Importable>> loadImportables(BufferedReader reader)
      throws IOException {
    Map<String, List<Importable>> importables = new HashMap<>();
    String line;
    while ((line = reader.readLine()) != null) {
      Matcher m = importablePattern.matcher(line);
      if (!m.matches()) {
        continue;
      }

      Importable importable = loadImportable(m);
      List<Importable> importablesOfSameName = importables.get(importable.name);
      if (importablesOfSameName == null) {
        importablesOfSameName = new ArrayList<>();
      }

      importablesOfSameName.add(importable);
      importables.put(importable.name, importablesOfSameName);
    }

    return importables;
  }

  private static Importable loadImportable(Matcher match) {
    Importable importable = new Importable();
    String pkg = match.group("pkg");
    String className = match.group("class");
    String identifier = match.group("identifier");
    if (identifier == null) {
      importable.isStatic = false;
      importable.pkg = pkg;
      importable.name = className;
      return importable;
    }

    // in the case of a static import, we want to be able to address it by its identifier and not
    // className.identifier
    importable.isStatic = true;
    importable.pkg = String.join(".", pkg, className);
    importable.name = identifier;
    return importable;
  }

  private static void output(JavaApi api) throws FormatterException, IOException {
    String fileName = getFileName(api);
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);
    outputHeader(out, fileName);
    for (Map.Entry<String, List<Importable>> importablesOfName : api.importables.entrySet()) {
      out.printf(".put(\"%s\", new Import[] {", importablesOfName.getKey());
      for (Importable i : importablesOfName.getValue()) {
        i.output(out);
        out.print(",");
      }
      out.print("})\n");
    }

    outputFooter(out);
    // String formatted = new Formatter().formatSourceAndFixImports(writer.toString());
    String formatted = writer.toString();
    File f =
        new File(
            "core/src/main/java/com/nikodoko/javaimports/stdlib/internal/" + fileName + ".java");
    Files.asCharSink(f, UTF_8).write(formatted);
  }

  private static String getFileName(JavaApi api) {
    return String.format("Java%sStdlib", api.version.replace(".", ""));
  }

  private static void outputHeader(PrintWriter out, String fileName) {
    out.println("// this has been auto generated by GenerateStdlib.java, do not modify");
    out.println("package com.nikodoko.javaimports.stdlib.internal;");
    out.println("import com.google.common.collect.ImmutableMap;");
    out.println("import com.nikodoko.javaimports.parser.Import;");
    out.println("import java.util.Map;");
    out.printf("public class %s implements Stdlib {\n", fileName);
    out.println("private static final Map<String, Import[]> CLASSES = new ImmutableMap.Builder()");
  }

  private static void outputFooter(PrintWriter out) {
    out.println(".build();");
    out.println("public Import[] getClassesFor(String identifier) {");
    out.println("return CLASSES.get(identifier);");
    out.println("}");
    out.println("}");
  }
}
