package com.nikodoko.javaimports.environment.shared;

import java.nio.file.Path;

/**
 * Represents a dependency provided by the environment. It can be direct or transitive, and holds a
 * path pointing to a JAR, a source file...
 */
public interface Dependency {
  enum Kind {
    DIRECT,
    TRANSITIVE;
  }

  Kind kind();

  Path path();
}
