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
  @Test
  public void testExtend() {
    ClassEntity child =
        new ClassEntity(Visibility.PUBLIC, false, "Child", ClassSelectors.of("Parent"));
    ClassEntity parent =
        new ClassEntity(Visibility.PUBLIC, false, "Parent").members(ImmutableSet.of("a", "b"));

    ClassHierarchy hierarchy = ClassHierarchies.root();
    hierarchy.moveTo(parent);

    ClassExtender extender = ClassExtender.of(child).notYetResolved(ImmutableSet.of("a", "b", "c"));
    extender.extendAsMuchAsPossibleUsing(hierarchy);

    assertThat(extender.isFullyExtended()).isTrue();
    assertThat(extender.notYetResolved()).containsExactlyElementsIn(ImmutableSet.of("c"));
  }
}
