package com.nikodoko.javaimports.environment.shared;

public interface Dependency {
  enum Kind {
    DIRECT,
    TRANSITIVE;
  }

  Kind kind();
}
