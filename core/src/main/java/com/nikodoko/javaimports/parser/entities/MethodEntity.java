package com.nikodoko.javaimports.parser.entities;

public class MethodEntity implements Entity {
  private final String name;
  private final Visibility visibility;
  private final boolean isStatic;

  MethodEntity(Visibility visibility, boolean isStatic, String name) {
    this.visibility = visibility;
    this.name = name;
    this.isStatic = isStatic;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Kind kind() {
    return Kind.METHOD;
  }
}
