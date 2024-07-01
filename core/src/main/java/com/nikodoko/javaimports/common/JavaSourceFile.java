package com.nikodoko.javaimports.common;

import java.util.Set;

public interface JavaSourceFile extends ImportProvider, ClassProvider {
  Selector pkg();

  Set<Identifier> topLevelDeclarations();
}
