package com.nikodoko.importer;

import com.google.common.base.MoreObjects;
import java.util.HashSet;
import java.util.Set;

/**
 * Maintains the set of named language entities delared in the scope, as well as a link to the
 * immediately surrounding (parent) scope.
 */
public class Scope {
  private Set<String> entities = new HashSet<>();
  private Scope parent;

  public Scope() {}

  public Scope parent() {
    return parent;
  }

  /**
   * Returns whether the identifier is found in this scope, ignoring all parent scopes.
   *
   * @param identifier the identifier to look for
   * @return whether the identifier was found
   */
  public boolean lookup(String identifier) {
    return entities.contains(identifier);
  }

  /**
   * Inserts the identifier in the scope.
   *
   * @param identifier the identifier to add
   */
  public void insert(String identifier) {
    entities.add(identifier);
  }

  /**
   * Debugging support.
   *
   * <p>This does not output anything about the parent scopes.
   */
  public String toString() {
    return MoreObjects.toStringHelper(this).add("entities", entities).toString();
  }
}
