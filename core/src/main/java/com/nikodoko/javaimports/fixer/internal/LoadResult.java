package com.nikodoko.javaimports.fixer.internal;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.OrphanClass;
import java.util.Set;

/** Contains the result of {@link Loader#load}. */
public class LoadResult {
  public Set<Identifier> unresolved;
  public Set<OrphanClass> orphans;

  /** TODO: Remove me and make me default */
  public Set<OrphanClass> orphans() {
    return orphans;
  }

  /** TODO: Remove me and make me default */
  public Set<Identifier> unresolved() {
    return unresolved;
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
