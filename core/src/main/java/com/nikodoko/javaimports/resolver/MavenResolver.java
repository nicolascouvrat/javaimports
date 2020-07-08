package com.nikodoko.javaimports.resolver;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.nikodoko.javaimports.ImporterException;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;
import com.nikodoko.javaimports.parser.ParserOptions;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MavenResolver implements Resolver {
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
  private boolean isInitialized = false;
  private ParserOptions parserOpts = ParserOptions.builder().debug(false).build();

  MavenResolver(Path root, Path fileBeingResolved) {
    this.root = root;
    this.fileBeingResolved = fileBeingResolved;
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
    List<List<ImportWithDistance>> files =
        javaFilesNotBeingResolved().map(this::extractImports).collect(Collectors.toList());

    for (List<ImportWithDistance> imports : files) {
      for (ImportWithDistance i : imports) {
        List<ImportWithDistance> importsForIdentifier =
            importsByIdentifier.getOrDefault(i.i.name(), new ArrayList<>());
        importsForIdentifier.add(i);
        importsByIdentifier.put(i.i.name(), importsForIdentifier);
      }
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

  private List<ImportWithDistance> extractImports(Path path) {
    ParsedFile file = parseFileAt(path);
    List<ImportWithDistance> imports = new ArrayList<>();
    for (String identifier : file.topLevelDeclarations()) {
      ImportWithDistance toAdd = new ImportWithDistance();
      toAdd.i = new Import(identifier, file.packageName(), false);
      toAdd.distance = distance(fileBeingResolved, path);
      imports.add(toAdd);
    }

    return imports;
  }

  private int distance(Path from, Path to) {
    return from.relativize(to).toString().split("/").length;
  }

  private ParsedFile parseFileAt(Path path) {
    try {
      String contents = new String(Files.readAllBytes(path), UTF_8);
      return new Parser(parserOpts).parse(path, contents);
    } catch (IOException | ImporterException e) {
      throw new RuntimeException(e);
    }
  }
}
