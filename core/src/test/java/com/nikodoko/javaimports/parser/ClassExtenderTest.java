package com.nikodoko.javaimports.parser;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.nikodoko.javaimports.parser.entities.ClassEntity;
import com.nikodoko.javaimports.parser.entities.ScopedClassEntity;
import com.nikodoko.javaimports.parser.entities.Visibility;
import org.junit.Test;

public class ClassExtenderTest {
  @Test
  public void testExtend() {
    ClassEntity child =
        new ClassEntity(Visibility.PUBLIC, false, "Child").parentPath(ImmutableList.of("Parent"));
    ClassEntity parent =
        new ClassEntity(Visibility.PUBLIC, false, "Parent").members(ImmutableSet.of("a", "b"));

    Scope scope = new Scope(null);
    scope.insert("Parent", ScopedClassEntity.of(parent));

    ClassExtender extender = ClassExtender.of(child).notYetResolved(ImmutableSet.of("a", "b", "c"));
    extender.extendAsMuchAsPossibleUsing(scope);

    assertThat(extender.isFullyExtended()).isTrue();
    assertThat(extender.notYetResolved()).containsExactlyElementsIn(ImmutableSet.of("c"));
  }
}
