package com.nikodoko.javaimports.parser;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.OrphanClass;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Superclass;
import com.nikodoko.javaimports.parser.internal.ClassEntity;
import com.nikodoko.javaimports.parser.internal.ClassSelectors;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ClassExtenderTest {
  ClassEntity childOfChild =
      ClassEntity.namedAndExtending("ChildOfChild", ClassSelectors.of("Child"));
  ClassEntity child =
      ClassEntity.namedAndExtending("Child", ClassSelectors.of("Parent"))
          .members(ImmutableSet.of("c"));
  ClassEntity parent = ClassEntity.named("Parent").members(ImmutableSet.of("a", "b"));
  ClassEntity otherClass =
      ClassEntity.namedAndExtending("Child", ClassSelectors.of("Other", "Parent"));

  @Test
  void testConversion() {
    ClassExtender extender =
        ClassExtender.of(otherClass).notYetResolved(ImmutableSet.of("a", "b", "c"));
    var got = extender.toOrphanClass();

    assertThat(got)
        .isEqualTo(
            new OrphanClass(
                Selector.of("Child"),
                Set.of(new Identifier("a"), new Identifier("b"), new Identifier("c")),
                Superclass.unresolved(Selector.of("Other", "Parent"))));
  }

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
