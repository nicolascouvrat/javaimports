package com.nikodoko.javaimports.parser.entities;

public class VariableEntity implements Entity {
  private Visibility visibility;
  private String name;
  private boolean isStatic;

  VariableEntity(Visibility visibility, boolean isStatic, String name) {
    this.visibility = visibility;
    this.isStatic = isStatic;
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Kind kind() {
    return Kind.VARIABLE;
  }
}
