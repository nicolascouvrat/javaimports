package com.nikodoko.javaimports.environment.shared;

import java.nio.file.Path;

public interface Dependency {
  enum Kind {
    DIRECT,
    TRANSITIVE;
  }

  Kind kind();

  Path path();
}
