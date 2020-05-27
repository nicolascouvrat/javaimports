package com.nikodoko.javaimports.parser;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.nikodoko.javaimports.parser.entities.ClassEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassExtender {
  private Set<String> notYetResolved = new HashSet<>();
  private ClassEntity toExtend;
  private List<String> nextParentPath;

  private ClassExtender(ClassEntity toExtend, List<String> nextParentPath) {
    this.toExtend = toExtend;
    this.nextParentPath = nextParentPath;
  }

  public static ClassExtender of(ClassEntity toExtend) {
    checkArgument(toExtend.isChildClass(), "ClassExtender only accept child classes");
    return new ClassExtender(toExtend, toExtend.parentPath());
  }

  public ClassExtender withNotYetResolved(Set<String> identifiers) {
    checkNotNull(identifiers, "ClassExtender does not accept null for unresolved identifiers");
    this.notYetResolved = identifiers;
    return this;
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
