package com.nikodoko.javaimports.fixer;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.parser.ClassExtender;
import java.util.Set;

// An intermediate result containing symbols that cannot be found and child classes that cannot be
// extended
class LoadResult {
  public Set<String> unresolved;
  public Set<ClassExtender> orphans;

  public LoadResult(Set<String> unresolved, Set<ClassExtender> orphans) {
    this.unresolved = unresolved;
    this.orphans = orphans;
  }

  public boolean isEmpty() {
    return unresolved.isEmpty() && orphans.isEmpty();
  }

  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("unresolved", unresolved)
        .add("orphans", orphans)
        .toString();
  }
}
