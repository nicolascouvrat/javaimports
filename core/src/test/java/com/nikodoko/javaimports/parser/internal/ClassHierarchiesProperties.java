package com.nikodoko.javaimports.parser.internal;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

public class ClassHierarchiesProperties {
  @Property
  boolean movingUpAsManyTimesAsDownReturnsTheSameNode(
      @ForAll @IntRange(min = 1, max = 100) int times) {
    ClassHierarchy start = ClassHierarchies.root();
    ClassHierarchy current = start;
    for (int i = 0; i < times; i++) {
      current = current.moveToLeaf();
    }

    for (int i = 0; i < times; i++) {
      current = current.moveUp();
    }

    return start == current;
  }
}
