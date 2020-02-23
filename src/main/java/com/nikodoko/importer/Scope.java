package com.nikodoko.importer;

import com.google.common.base.MoreObjects;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Name;

/**
 * Maintains the set of named language entities delared in the scope, as well as a link to the
 * immediately surrounding (parent) scope.
 *
 * <p>The top level scope should have {@code parent==null}.
 */
public class Scope {
  private Set<Name> entities = new HashSet<>();
  private Scope parent;

  /**
   * The {@code Scope} constructor.
   *
   * @param parent its parent scope
   */
  public Scope(Scope parent) {
    this.parent = parent;
  }

  public Scope parent() {
    return parent;
  }

  /**
   * Returns whether the identifier is found in this scope, ignoring all parent scopes.
   *
   * @param identifier the identifier to look for
   * @return whether the identifier was found
   */
  public boolean lookup(Name identifier) {
    return entities.contains(identifier);
  }

  /**
   * Inserts the identifier in the scope.
   *
   * @param identifier the identifier to add
   */
  public void insert(Name identifier) {
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
