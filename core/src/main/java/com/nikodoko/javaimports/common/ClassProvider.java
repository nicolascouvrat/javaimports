package com.nikodoko.javaimports.common;

import java.util.Optional;

@FunctionalInterface
public interface ClassProvider {
  public Optional<ClassEntity> findClass(Import i);
}
