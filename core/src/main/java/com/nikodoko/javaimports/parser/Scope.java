package com.nikodoko.javaimports.parser;

import com.google.common.base.MoreObjects;
import java.util.HashSet;
import java.util.Set;

/**
 * Maintains the set of named language entities delared in the scope, as well as a link to the
 * immediately surrounding (parent) scope.
 *
 * <p>Also maintains a set of unresolved identifiers found in this scope.
 *
 * <p>The top level scope should have {@code parent==null}.
 */
public class Scope {
  private Set<String> identifiers = new HashSet<>();
  private Set<String> notYetResolved = new HashSet<>();
  // Parent scope, can be null if top scope
  private Scope parent = null;
  private Set<ClassExtender> notFullyExtended = new HashSet<>();

  /**
   * The {@code Scope} constructor.
   *
   * @param parent its parent scope
   */
  public Scope(Scope parent) {
    this.parent = parent;
  }

  /** This {@code Scope}'s parent scope. */
  public Scope parent() {
    return parent;
  }

  /** The set of identifiers that have not yet been resolved in this scope */
  public Set<String> notYetResolved() {
    return notYetResolved;
  }

  /**
   * Sets this {@code Scope}'s notYetResolved set.
   *
   * @param identifiers the new set
   */
  public void notYetResolved(Set<String> identifiers) {
    notYetResolved = identifiers;
  }

  public Set<ClassExtender> notFullyExtended() {
    return notFullyExtended;
  }

  public Set<String> identifiers() {
    return identifiers;
  }

  /**
   * Returns whether the identifier is found in this scope, ignoring all parent scopes.
   *
   * @param identifier the identifier to look for
   * @return the entity found, or null
   */
  boolean contains(String identifier) {
    return identifiers.contains(identifier);
  }

  /**
   * Inserts the identifier in the scope.
   *
   * @param identifier the identifier to add
   */
  void insert(String identifier) {
    identifiers.add(identifier);
  }

  /** Adds the identifier to the set of identifiers that have not yet been resolved */
  public void markAsNotYetResolved(String identifier) {
    notYetResolved.add(identifier);
  }

  public void markAsNotFullyExtended(ClassExtender extender) {
    notFullyExtended.add(extender);
  }

  /**
   * Debugging support.
   *
   * <p>This does not output anything about the parent scopes.
   */
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("identifiers", identifiers)
        .add("notYetResolved", notYetResolved)
        .toString();
  }
}
