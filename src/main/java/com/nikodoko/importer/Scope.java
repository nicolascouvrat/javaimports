package com.nikodoko.importer;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.Name;

/**
 * Maintains the set of named language entities delared in the scope, as well as a link to the
 * immediately surrounding (parent) scope.
 *
 * <p>Also maintains a set of unresolved identifiers found in this scope.
 *
 * <p>The top level scope should have {@code parent==null}.
 */
public class Scope {
  // All the identifiers declared so far in this scope
  private Set<Name> entities = new HashSet<>();
  // All the public and protected identifiers declared in this scope
  private Set<Name> exportedEntities = new HashSet<>();

  // All the identifiers so far unresolved in this scope
  // It has to be thread safe, as we might remove elements from multiple threads
  private Set<Name> unresolved = Sets.newConcurrentHashSet();
  // Only for classes extending another one.
  private Class extendedClass = null;
  // Parent scope, can be null if top scope
  private Scope parent = null;

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
   * Returns whether the identifier is found in this scope's exported identifiers, ignoring all
   * parent scopes.
   *
   * @param identifier the identifier to look for
   * @return whether the identifier was found
   */
  public boolean lookupExported(Name identifier) {
    return exportedEntities.contains(identifier);
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
   * Inserts the identifier in the scope's exported identifiers.
   *
   * @param identifier the identifier to add
   */
  public void insertExported(Name identifier) {
    exportedEntities.add(identifier);
  }

  /**
   * Inserts the identifier as one currently unresolved
   *
   * @param identifier the identifier to add
   */
  public void markAsUnresolved(Name identifier) {
    unresolved.add(identifier);
  }

  /**
   * Removes the identifier from the unresolved set
   *
   * @param identifier the resolved identifier
   */
  public void resolve(Name identifier) {
    boolean ok = unresolved.remove(identifier);
    if (!ok) {
      throw new IllegalArgumentException(
          "why did we try to resolve an identifier not in this scope?!");
    }
  }

  /**
   * Registers an extended class
   *
   * @param extendedClass the extended class
   */
  public void registerExtendedClass(Class extendedClass) {
    this.extendedClass = extendedClass;
  }

  public boolean hasExtends() {
    return extendedClass != null;
  }

  public Class extendedClass() {
    return extendedClass;
  }

  public Set<Name> unresolved() {
    return unresolved;
  }

  /**
   * Debugging support.
   *
   * <p>This does not output anything about the parent scopes.
   */
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("entities", entities)
        .add("unresolved", unresolved)
        .add("exportedEntities", exportedEntities)
        .add("extendedClass", extendedClass)
        .toString();
  }
}
