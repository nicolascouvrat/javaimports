package com.nikodoko.javaimports.parser;

import static com.google.common.base.Preconditions.checkArgument;

import com.nikodoko.javaimports.parser.entities.ClassEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassExtender {
  private Set<String> notYetResolved = new HashSet<>();
  private ClassEntity toExtend;
  private List<String> nextParentPath;

  private ClassExtender(
      ClassEntity toExtend, Set<String> notYetResolved, List<String> nextParentPath) {
    this.toExtend = toExtend;
    this.notYetResolved = notYetResolved;
    this.nextParentPath = nextParentPath;
  }

  public static ClassExtender of(ClassEntity toExtend, Set<String> notYetResolved) {
    checkArgument(toExtend.isChildClass(), "can only create extender of child classes");
    return new ClassExtender(toExtend, notYetResolved, toExtend.parentPath());
  }

  public void resolveUsing(Scope scope) {
    Set<String> unresolved = new HashSet<>();
    for (String s : notYetResolved) {
      if (scope.lookup(s) == null) {
        unresolved.add(s);
      }
    }

    notYetResolved = unresolved;
  }

  public Set<String> notYetResolved() {
    return notYetResolved;
  }
}
