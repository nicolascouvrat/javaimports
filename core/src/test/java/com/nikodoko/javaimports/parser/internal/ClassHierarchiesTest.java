package com.nikodoko.javaimports.parser.internal;

import static com.google.common.truth.Truth8.assertThat;

import com.nikodoko.javaimports.parser.entities.ClassEntity;
import com.nikodoko.javaimports.parser.entities.Visibility;
import org.junit.jupiter.api.Test;

public class ClassHierarchiesTest {
  @Test
  void testClassIsFound() {
    ClassEntity A = new ClassEntity(Visibility.PUBLIC, true, "A");
    ClassEntity B = new ClassEntity(Visibility.PUBLIC, true, "B");
    ClassEntity C = new ClassEntity(Visibility.PUBLIC, true, "C");

    ClassHierarchy hierarchy = ClassHierarchies.root();
    ClassHierarchy root = hierarchy;
    hierarchy = hierarchy.moveTo(A);
    hierarchy = hierarchy.moveTo(B);
    hierarchy = hierarchy.moveToLeaf();
    hierarchy = hierarchy.moveTo(C);

    assertThat(root.find(ClassSelectors.of("A"))).hasValue(A);
  }
}
