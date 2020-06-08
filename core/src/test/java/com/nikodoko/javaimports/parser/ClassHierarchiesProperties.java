package com.nikodoko.javaimports.parser;

import static com.google.common.truth.Truth8.assertThat;

import com.nikodoko.javaimports.parser.internal.ClassSelectors;
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
      current = current.moveUp().get();
    }

    return start == current;
  }

  @Property
  void rootDoesNotContainAnything(@ForAll String aSelector) {
    ClassHierarchy root = ClassHierarchies.root();
    assertThat(root.find(ClassSelectors.of(aSelector))).isEmpty();
  }

  @Property
  void emptyCombinationDoesNotContainAnything(@ForAll String aSelector) {
    ClassHierarchy combined = ClassHierarchies.combine();
    assertThat(combined.find(ClassSelectors.of(aSelector))).isEmpty();
  }
}
