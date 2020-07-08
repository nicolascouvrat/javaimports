package com.nikodoko.javaimports.resolver;

import com.google.common.annotations.VisibleForTesting;
import com.nikodoko.javaimports.parser.Import;
import java.nio.file.Path;
import java.util.Optional;

public class MavenResolver implements Resolver {
  private final Path root;

  @Override
  public Optional<Import> find(String identifier) {
    return null;
  }

  MavenResolver(Path root) {
    this.root = root;
  }

  @VisibleForTesting
  Path root() {
    return root;
  }
}
