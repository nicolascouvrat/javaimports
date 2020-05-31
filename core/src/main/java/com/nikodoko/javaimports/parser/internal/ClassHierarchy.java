package com.nikodoko.javaimports.parser.internal;

import com.nikodoko.javaimports.parser.entities.ClassEntity;
import java.util.Optional;

public interface ClassHierarchy {
  public ClassHierarchy moveTo(ClassEntity child);

  public ClassHierarchy moveToLeaf();

  public Optional<ClassHierarchy> moveUp();

  public Optional<ClassEntity> find(ClassSelector selector);
}
