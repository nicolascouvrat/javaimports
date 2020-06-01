package com.nikodoko.javaimports.parser;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import com.nikodoko.javaimports.parser.entities.ClassEntity;
import com.nikodoko.javaimports.parser.entities.Visibility;
import com.nikodoko.javaimports.parser.internal.ClassHierarchies;
import com.nikodoko.javaimports.parser.internal.ClassHierarchy;
import com.nikodoko.javaimports.parser.internal.ClassSelectors;
import org.junit.jupiter.api.Test;

public class ClassExtenderTest {
  ClassEntity childOfChild =
      new ClassEntity(Visibility.PUBLIC, false, "ChildOfChild", ClassSelectors.of("Child"));
  ClassEntity child =
      new ClassEntity(Visibility.PUBLIC, false, "Child", ClassSelectors.of("Parent"))
          .members(ImmutableSet.of("c"));
  ClassEntity parent =
      new ClassEntity(Visibility.PUBLIC, false, "Parent").members(ImmutableSet.of("a", "b"));

  @Test
  void testExtendChildClass() {
    ClassHierarchy hierarchy = createFlatHierarchy(parent);
    ClassExtender extender = ClassExtender.of(child).notYetResolved(ImmutableSet.of("a", "b", "c"));

    extender.extendAsMuchAsPossibleUsing(hierarchy);

    assertThat(extender.isFullyExtended()).isTrue();
    assertThat(extender.notYetResolved()).containsExactlyElementsIn(ImmutableSet.of("c"));
  }

  @Test
  void testExtendChildOfChildClass() {
    ClassHierarchy hierarchy = createFlatHierarchy(parent, child);
    ClassExtender extender =
        ClassExtender.of(childOfChild).notYetResolved(ImmutableSet.of("a", "b", "c", "d"));

    extender.extendAsMuchAsPossibleUsing(hierarchy);

    assertThat(extender.isFullyExtended()).isTrue();
    assertThat(extender.notYetResolved()).containsExactlyElementsIn(ImmutableSet.of("d"));
  }

  @Test
  void testExtendNotAChildClass() {
    ClassHierarchy hierarchy = createFlatHierarchy();
    ClassExtender extender = ClassExtender.of(parent).notYetResolved(ImmutableSet.of("c", "d"));

    extender.extendAsMuchAsPossibleUsing(hierarchy);

    assertThat(extender.isFullyExtended()).isTrue();
    assertThat(extender.notYetResolved()).containsExactlyElementsIn(ImmutableSet.of("c", "d"));
  }

  @Test
  void testPartialExtensionOfChildClass() {
    ClassHierarchy hierarchy = createFlatHierarchy(child);
    ClassExtender extender =
        ClassExtender.of(childOfChild).notYetResolved(ImmutableSet.of("a", "b", "c"));

    extender.extendAsMuchAsPossibleUsing(hierarchy);

    assertThat(extender.isFullyExtended()).isFalse();
    assertThat(extender.notYetResolved()).containsExactlyElementsIn(ImmutableSet.of("a", "b"));
  }

  static ClassHierarchy createFlatHierarchy(ClassEntity... entities) {
    ClassHierarchy hierarchy = ClassHierarchies.root();
    for (ClassEntity entity : entities) {
      hierarchy.moveTo(entity);
    }

    return hierarchy;
  }
}
