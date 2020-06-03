package com.nikodoko.javaimports.parser.internal;

import static com.google.common.truth.Truth8.assertThat;

import com.nikodoko.javaimports.parser.entities.ClassEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ClassHierarchiesTest {
  static ClassHierarchy root;
  static ClassHierarchy leaf;
  static ClassEntity A = new ClassEntity("A");
  static ClassEntity B = new ClassEntity("B");
  static ClassEntity C1 = new ClassEntity("C1");
  static ClassEntity C2 = new ClassEntity("C2");

  // Create the following:
  // root
  // |
  // - A
  //   |
  //   - B
  //
  // leaf
  // |
  // - C1
  // - C2
  @BeforeAll
  static void setupHierarchy() {
    root = ClassHierarchies.root();

    ClassHierarchy hierarchy = root;
    hierarchy = hierarchy.moveTo(A);
    hierarchy = hierarchy.moveTo(B);
    hierarchy = hierarchy.moveToLeaf();
    leaf = hierarchy;
    hierarchy = hierarchy.moveTo(C1);
    hierarchy = hierarchy.moveUp().get();
    hierarchy = hierarchy.moveTo(C2);
  }

  @Test
  void testFound() {
    assertThat(root.find(ClassSelectors.of("A"))).hasValue(A);
    assertThat(root.find(ClassSelectors.of("A", "B"))).hasValue(B);
  }

  @Test
  void testNotFound() {
    assertThat(root.find(ClassSelectors.of("Z"))).isEmpty();
  }

  @Test
  void testLeafNotReachableFromParent() {
    assertThat(root.find(ClassSelectors.of("A", "B", "C1"))).isEmpty();
    assertThat(root.find(ClassSelectors.of("A", "B", "C2"))).isEmpty();
  }

  @Test
  void testLeafChildsCanReachEachother() {
    assertThat(leaf.find(ClassSelectors.of("C1"))).hasValue(C1);
    assertThat(leaf.find(ClassSelectors.of("C2"))).hasValue(C2);
  }

  @Test
  void testCombineLeafAndRoot() {
    ClassHierarchy combined = ClassHierarchies.combine(root, leaf);

    assertThat(leaf.find(ClassSelectors.of("C1"))).hasValue(C1);
    assertThat(leaf.find(ClassSelectors.of("C2"))).hasValue(C2);
    assertThat(root.find(ClassSelectors.of("A"))).hasValue(A);
    assertThat(root.find(ClassSelectors.of("A", "B"))).hasValue(B);
  }
}
