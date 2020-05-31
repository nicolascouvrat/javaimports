package com.nikodoko.javaimports.parser.internal;

import com.nikodoko.javaimports.parser.entities.ClassEntity;

public interface ClassHierarchy {
  public ClassHierarchy moveTo(ClassEntity child);

  public ClassHierarchy moveToLeaf();

  public ClassHierarchy moveUp();
}
