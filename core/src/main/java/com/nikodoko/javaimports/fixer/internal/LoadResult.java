package com.nikodoko.javaimports.fixer.internal;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.parser.ClassExtender;
import java.util.Set;

/** Contains the result of {@link Loader#load}. */
public class LoadResult {
  public Set<String> unresolved;
  public Set<ClassExtender> orphans;
  public Candidates candidates = new Candidates();

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
