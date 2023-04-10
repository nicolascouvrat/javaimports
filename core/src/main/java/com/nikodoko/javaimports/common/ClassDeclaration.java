package com.nikodoko.javaimports.common;

import java.util.Optional;

public record ClassDeclaration(Selector name, Optional<Superclass> maybeParent) {
  public ClassDeclaration(Selector name, Superclass parent) {
    this(name, Optional.of(parent));
  }

  public ClassDeclaration addParent(Optional<Superclass> maybeNext) {
    return new ClassDeclaration(name(), maybeNext);
  }
}
