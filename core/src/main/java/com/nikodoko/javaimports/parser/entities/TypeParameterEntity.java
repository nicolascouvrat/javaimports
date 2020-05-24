package com.nikodoko.javaimports.parser.entities;

public class TypeParameterEntity implements Entity {
  private String name;

  TypeParameterEntity(String name) {
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Kind kind() {
    return Kind.TYPE_PARAMETER;
  }
}
