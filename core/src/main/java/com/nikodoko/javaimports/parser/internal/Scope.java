package com.nikodoko.javaimports.parser.internal;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.parser.ClassExtender;
import java.util.HashSet;
import java.util.Set;

/**
 * Maintains the set of identifiers delared in the scope, as well as a link to the immediately
 * surrounding (parent) scope.
 *
 * <p>Also maintains a set of unresolved identifiers found in this scope.
 *
 * <p>The top level scope should have {@code parent==null}.
 */
public class Scope {
  public Set<String> identifiers = new HashSet<>();
  public Set<String> notYetResolved = new HashSet<>();
  // Parent scope can be null if top scope
  public Scope parent = null;
  public Set<ClassExtender> notFullyExtended = new HashSet<>();

  /**
   * Debugging support.
   *
   * <p>This does not output anything about the parent scopes.
   */
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("identifiers", identifiers)
        .add("notYetResolved", notYetResolved)
        .add("notFullyExtended", notFullyExtended)
        .toString();
  }
}
