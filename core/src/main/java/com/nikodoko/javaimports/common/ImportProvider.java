package com.nikodoko.javaimports.common;

import java.util.Collection;

@FunctionalInterface
public interface ImportProvider {
  public Collection<Import> findImports(Identifier i);
}
