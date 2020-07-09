package com.nikodoko.javaimports.resolver;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.nikodoko.javaimports.ImporterException;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;
import com.nikodoko.javaimports.parser.ParserOptions;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MavenResolver implements Resolver {
  private static class JavaFile {
    ParsedFile contents;
    Path path;
  }

  private static class ImportWithDistance implements Comparable<ImportWithDistance> {
    Import i;
    // The distance to an imports is defined as the length of the relative path between the file
    // being resolved and the directory containing this import.
    int distance;

    @Override
    public int compareTo(ImportWithDistance other) {
      if (other.distance == distance) {
        return 0;
      }

      if (distance > other.distance) {
        return 1;
      }

      return -1;
    }
  }

  private final Path root;
  private final Path fileBeingResolved;
  private Map<String, List<ImportWithDistance>> importsByIdentifier = new HashMap<>();
  private List<JavaFile> filesInProject = new ArrayList<>();
  private boolean isInitialized = false;

  MavenResolver(Path root, Path fileBeingResolved) {
    this.root = root;
    this.fileBeingResolved = fileBeingResolved;
  }

  @Override
  public Set<ParsedFile> filesInPackage(String packageName) {
    if (!isInitialized) {
      init();
    }

    Set<ParsedFile> files = new HashSet<>();
    for (JavaFile file : filesInProject) {
      if (file.contents.packageName().equals(packageName)) {
        files.add(file.contents);
      }
    }

    return files;
  }

  @Override
  public Optional<Import> find(String identifier) {
    if (!isInitialized) {
      init();
    }

    List<ImportWithDistance> imports = importsByIdentifier.get(identifier);
    if (imports == null) {
      return Optional.empty();
    }

    return Optional.of(Collections.min(imports).i);
  }

  private void init() {
    filesInProject = javaFilesNotBeingResolved().map(this::parseFile).collect(Collectors.toList());

    for (JavaFile file : filesInProject) {
      for (ImportWithDistance i : extractImports(file)) {
        List<ImportWithDistance> importsForIdentifier =
            importsByIdentifier.getOrDefault(i.i.name(), new ArrayList<>());
        importsForIdentifier.add(i);
        importsByIdentifier.put(i.i.name(), importsForIdentifier);
      }
    }
  }

  private List<MavenDependency> findAllDependencies() {
    MavenDependencyFinder finder = new MavenDependencyFinder();
    scanPom(root, finder);

    Path target = root.getParent();
    while (target != null && !finder.allFound()) {
      if (!hasPom(target)) {
        break;
      }

      scanPom(target, finder);
      target = target.getParent();
    }

    return finder.result();
  }

  private boolean hasPom(Path directory) {
    Path pom = Paths.get(directory.toString(), "pom.xml");
    return Files.exists(pom);
  }

  private void scanPom(Path directory, MavenDependencyFinder finder) {
    Path pom = Paths.get(directory.toString(), "pom.xml");
    try (FileReader reader = new FileReader(pom.toFile())) {
      finder.scan(reader);
    } catch (IOException e) {
      throw new IOError(e);
    }
  }

  private Stream<Path> javaFilesNotBeingResolved() {
    try {
      return Files.find(
          root,
          100,
          (path, attributes) ->
              path.toString().endsWith(".java") && !path.equals(fileBeingResolved));
    } catch (IOException e) {
      throw new IOError(e);
    }
  }

  private List<ImportWithDistance> extractImports(JavaFile file) {
    List<ImportWithDistance> imports = new ArrayList<>();
    for (String identifier : file.contents.topLevelDeclarations()) {
      ImportWithDistance toAdd = new ImportWithDistance();
      toAdd.i = new Import(identifier, file.contents.packageName(), false);
      toAdd.distance = distance(fileBeingResolved, file.path);
      imports.add(toAdd);
    }

    return imports;
  }

  private int distance(Path from, Path to) {
    return from.relativize(to).toString().split("/").length;
  }

  private JavaFile parseFile(Path path) {
    try {
      String source = new String(Files.readAllBytes(path), UTF_8);
      JavaFile file = new JavaFile();
      file.contents = new Parser(ParserOptions.builder().debug(false).build()).parse(path, source);
      file.path = path;
      return file;
    } catch (IOException | ImporterException e) {
      throw new RuntimeException(e);
    }
  }
}
