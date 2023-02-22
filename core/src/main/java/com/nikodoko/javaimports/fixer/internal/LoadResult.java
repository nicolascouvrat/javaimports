package com.nikodoko.javaimports.fixer.internal;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.OrphanClass;
import com.nikodoko.javaimports.parser.ClassExtender;
import java.util.Set;
import java.util.stream.Collectors;

/** Contains the result of {@link Loader#load}. */
public class LoadResult {
  public Set<Identifier> unresolved;
  public Set<ClassExtender> orphans;

  /** TODO: Remove me and make me default */
  public Set<OrphanClass> orphans() {
    return orphans.stream().map(ClassExtender::toOrphanClass).collect(Collectors.toSet());
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
