package com.nikodoko.javaimports.fixer.internal;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.parser.Orphans;
import java.util.Set;

/** Contains the result of {@link Loader#load}. */
public class LoadResult {
  public Set<Identifier> unresolved;
  public Orphans orphans;

  /** TODO: Remove me and make me default */
  public Orphans orphans() {
    return orphans;
  }

  /** TODO: Remove me and make me default */
  public Set<Identifier> unresolved() {
    return unresolved;
  }

  public boolean isEmpty() {
    return unresolved.isEmpty() && !orphans.needsParents();
  }

  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("unresolved", unresolved)
        .add("orphans", orphans)
        .toString();
  }
}
