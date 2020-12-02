package com.nikodoko.javaimports.common;

@FunctionalInterface
public interface ImportProvider {
  public Iterable<Import> findImports(Identifier i);
}
