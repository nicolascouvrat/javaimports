package com.nikodoko.javaimports.parser.entities;

public class ClassEntity implements Entity {
  @Override
  public String name() {
    return null;
  }

  @Override
  public Kind kind() {
    return Kind.CLASS;
  }
}
