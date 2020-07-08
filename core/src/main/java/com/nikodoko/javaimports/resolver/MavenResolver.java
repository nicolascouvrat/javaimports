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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MavenResolver implements Resolver {
  private final Path root;
  private Map<String, Import> imports = new HashMap<>();
  private boolean isInitialized = false;
  private ParserOptions parserOpts = ParserOptions.builder().debug(false).build();

  @Override
  public Optional<Import> find(String identifier) {
    if (!isInitialized) {
      try {
        init();
      } catch (IOException e) {
        throw new IOError(e);
      }
    }

    return Optional.ofNullable(imports.get(identifier));
  }

  MavenResolver(Path root) {
    this.root = root;
  }

  private void init() throws IOException {
    Stream<Path> paths =
        Files.find(root, 100, (path, attributes) -> path.toString().endsWith(".java"));
    List<ParsedFile> files = paths.map(this::parseFileAt).collect(Collectors.toList());

    for (ParsedFile f : files) {
      for (String identifier : f.topLevelDeclarations()) {
        imports.put(identifier, new Import(identifier, f.packageName(), false));
      }
    }
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
