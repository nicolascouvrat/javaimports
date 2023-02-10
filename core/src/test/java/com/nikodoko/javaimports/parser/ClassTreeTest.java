package com.nikodoko.javaimports.parser;

import static com.google.common.truth.Truth8.assertThat;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Selector;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ClassTreeTest {
  // by nikodoko.com
  static final ClassEntity ROOT = ClassEntity.named(Selector.of("Root")).build();
  static final ClassEntity A = ClassEntity.named(Selector.of("A")).build();
  static final ClassEntity B = ClassEntity.named(Selector.of("B")).build();
  static final ClassEntity C = ClassEntity.named(Selector.of("C")).build();
  static final ClassEntity D1 = ClassEntity.named(Selector.of("D1")).build();
  static final ClassEntity D2 = ClassEntity.named(Selector.of("D2")).build();
  static ClassTree root;
  static ClassTree functionRoot;

  // Create the following:
  // root
  // |
  // - A
  // | |
  // | - B
  // |
  // - C
  //   |
  //   - functionRoot (i.e. a function declaring local classes)
  //     |
  //     - C1
  //     - C2
  @BeforeAll
  static void setupTree() {
    var tree = ClassTree.root(ROOT);
    root = tree;
    tree = tree.pushAndMoveDown(A);
    tree = tree.pushAndMoveDown(B);
    tree = tree.moveUp();
    tree = tree.moveUp();
    tree = tree.pushAndMoveDown(C);
    tree = tree.moveDown();
    functionRoot = tree;
    tree = tree.pushAndMoveDown(D1);
    tree = tree.moveUp();
    tree = tree.pushAndMoveDown(D2);
  }

  @Test
  void testFound() {
    assertThat(root.find(Selector.of("A"))).hasValue(A);
    assertThat(root.find(Selector.of("A", "B"))).hasValue(B);
    assertThat(root.find(Selector.of("C"))).hasValue(C);
    assertThat(root.find(Selector.of("Root"))).hasValue(ROOT);
  }

  @Test
  void testNotFound() {
    assertThat(root.find(Selector.of("Z"))).isEmpty();
  }

  @Test
  void testClassesInFunctionRootNotReachableFromParent() {
    assertThat(root.find(Selector.of("C", "D1"))).isEmpty();
    assertThat(root.find(Selector.of("C", "D2"))).isEmpty();
  }

  @Test
  void testFunctionRootChildsCanReachEachother() {
    assertThat(functionRoot.find(Selector.of("D1"))).hasValue(D1);
    assertThat(functionRoot.find(Selector.of("D2"))).hasValue(D2);
  }

  @Test
  void testFlatView() {
    var got = root.flatView();
    com.google.common.truth.Truth.assertThat(got)
        .isEqualTo(
            Map.of(
                Selector.of("Root"), ROOT,
                Selector.of("Root", "A"), A,
                Selector.of("Root", "A", "B"), B,
                Selector.of("Root", "C"), C));
  }
}
