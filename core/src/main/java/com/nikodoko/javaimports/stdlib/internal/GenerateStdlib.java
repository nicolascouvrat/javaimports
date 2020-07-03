package com.nikodoko.javaimports.stdlib.internal;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.MoreObjects;
import com.google.common.io.Files;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;
import com.google.googlejavaformat.java.Formatter;
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

  private static final String prefix =
      "core/src/main/java/com/nikodoko/javaimports/stdlib/internal/";
  private static final int MAX_CHUNK_SIZE = 500;

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
    List<Map<String, List<Importable>>> chunks = new ArrayList<>();
    int i = 0;
    Map<String, List<Importable>> chunk = new HashMap<>();
    for (Map.Entry<String, List<Importable>> importablesOfName : api.importables.entrySet()) {
      chunk.put(importablesOfName.getKey(), importablesOfName.getValue());
      i++;
      if (i == MAX_CHUNK_SIZE) {
        chunks.add(chunk);
        chunk = new HashMap<>();
        i = 0;
      }
    }

    chunks.add(chunk);
    for (int j = 0; j < chunks.size(); j++) {
      String chunkFileName = getChunkFileName(fileName, j);
      outputChunkFile(chunkFileName, chunks.get(j));
      out.printf("classes = %s.CLASSES.get(identifier);\n", chunkFileName);
      out.println("if (classes != null) {");
      out.println("return classes;");
      out.println("}");
    }

    outputFooter(out);
    String formatted = new Formatter().formatSourceAndFixImports(writer.toString());
    File f = new File(prefix + fileName + ".java");
    Files.asCharSink(f, UTF_8).write(formatted);
  }

  private static String getFileName(JavaApi api) {
    return String.format("Java%sStdlib", api.version.replace(".", ""));
  }

  private static void outputHeader(PrintWriter out, String fileName) {
    out.println("// this has been auto generated by GenerateStdlib.java, do not modify");
    out.println("package com.nikodoko.javaimports.stdlib.internal;");
    out.println("import com.nikodoko.javaimports.parser.Import;");
    out.printf("public class %s implements Stdlib {\n", fileName);
    out.println("public Import[] getClassesFor(String identifier) {");
    out.println("Import[] classes = null;");
  }

  private static void outputFooter(PrintWriter out) {
    out.println("return null;");
    out.println("}");
    out.println("}");
  }

  private static String getChunkFileName(String fileName, int chunkIdx) {
    return String.format("%sChunk%d", fileName, chunkIdx);
  }

  private static void outputChunkFile(String chunkFileName, Map<String, List<Importable>> chunk)
      throws FormatterException, IOException {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);
    outputChunkFileHeader(out, chunkFileName);
    for (Map.Entry<String, List<Importable>> importablesOfName : chunk.entrySet()) {
      out.printf(".put(\"%s\", new Import[] {", importablesOfName.getKey());
      for (Importable i : importablesOfName.getValue()) {
        i.output(out);
        out.print(",");
      }
      out.print("})\n");
    }
    outputChunkFileFooter(out);
    String formatted = new Formatter().formatSourceAndFixImports(writer.toString());
    File f = new File(prefix + chunkFileName + ".java");
    Files.asCharSink(f, UTF_8).write(formatted);
  }

  private static void outputChunkFileHeader(PrintWriter out, String fileName) {
    out.println("// this has been auto generated by GenerateStdlib.java, do not modify");
    out.println("package com.nikodoko.javaimports.stdlib.internal;");
    out.println("import com.google.common.collect.ImmutableMap;");
    out.println("import com.nikodoko.javaimports.parser.Import;");
    out.println("import java.util.Map;");
    out.printf("public class %s {\n", fileName);
    out.println(
        "static final Map<String, Import[]> CLASSES = new ImmutableMap.Builder<String, Import[]>()");
  }

  private static void outputChunkFileFooter(PrintWriter out) {
    out.println(".build();");
    out.println("}");
  }
}
