package com.nikodoko.javaimports.parser;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.parser.entities.Entity;
import com.nikodoko.javaimports.parser.entities.Kind;
import com.nikodoko.javaimports.parser.entities.ScopedClassEntity;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
  // All the entities declared in this scope
  private Map<String, Entity> entities = new HashMap<>();
  // All the identifiers so far unresolved in this scope
  private Set<String> notYetResolved = new HashSet<>();
  // All the classes extending a parent for which that parent has not yet been found in this scope
  private Set<ScopedClassEntity> notYetExtended = new HashSet<>();
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

  /**
   * The set of classes extending a parent for which that parent has not yet been found in this
   * scope
   */
  public Set<ScopedClassEntity> notYetExtended() {
    return notYetExtended;
  }

  public Map<String, Entity> entities() {
    return entities;
  }

  /**
   * Returns whether the identifier is found in this scope, ignoring all parent scopes.
   *
   * @param identifier the identifier to look for
   * @return the entity found, or null
   */
  public Entity lookup(String identifier) {
    return entities.get(identifier);
  }

  /**
   * Inserts the identifier in the scope.
   *
   * @param identifier the identifier to add
   */
  public void insert(String identifier, Entity entity) {
    entities.put(identifier, entity);
  }

  // FIXME: get rid of me, I'm ugly
  private ScopedClassEntity unsafeCast(Entity classEntity) {
    if (classEntity == null) {
      return null;
    }

    checkArgument(
        classEntity instanceof ScopedClassEntity,
        "expected a class entity but got %s",
        classEntity.kind());
    return (ScopedClassEntity) classEntity;
  }

  /**
   * Searches for the parent class of {@code classEntity} in this {@code Scope}.
   *
   * <p>This will return {@code null} if the parent is not in the scope but has a chance to be
   * somewhere else, and an {@link Entity} with {@link Kind#BAD} if the parent class is not found
   * but cannot be found somewhere else (in other words, when it catches an identifier resolution
   * error).
   *
   * <p>{@code classEntity} is expected to be of {@link Kind#CLASS} and to have a non-null extended
   * path.
   *
   * @param classEntity the child class to try to extend
   */
  public ScopedClassEntity findParent(ScopedClassEntity classEntity) {
    checkArgument(classEntity.isChildClass(), "expected a child class entity");
    List<String> parentPath = classEntity.parentPath();
    return findParent(parentPath);
  }

  public ScopedClassEntity findParent(List<String> parentPath) {
    // The parentPath may look like something like this: A.B.C
    // What we are going to do:
    //  - See if the leftmost part of the path is in this scope (A in our case)
    //  - If not we might find it in another scope, so we return null
    //  - If yes we go down the path left to right, as long as we keep finding classes. If for
    //  whatever reason we do not (either because the class does not exist, or because it's not a
    //  class but something else), then we return a BAD Entity
    ScopedClassEntity maybeParent = unsafeCast(lookup(parentPath.get(0)));
    if (maybeParent == null) {
      return null;
    }

    Scope toScan = this;
    for (String s : parentPath) {
      maybeParent = unsafeCast(toScan.lookup(s));
      // FIXME: this might crash a little too aggressively? We could probably return a more helpful
      // error like "xxx is not a class"
      checkNotNull(maybeParent, "cannot find parent");

      toScan = maybeParent.scope();
    }

    // If we got here, then we found it
    return maybeParent;
  }

  /** Adds the class entity to the set of classes which parent has not yet been found */
  public void markAsNotYetExtended(ScopedClassEntity entity) {
    notYetExtended.add(entity);
  }

  /** Adds the identifier to the set of identifiers that have not yet been resolved */
  public void markAsNotYetResolved(String identifier) {
    notYetResolved.add(identifier);
  }

  /**
   * Debugging support.
   *
   * <p>This does not output anything about the parent scopes.
   */
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("entities", entities)
        .add("notYetResolved", notYetResolved)
        .add("notYetExtended", notYetExtended)
        .toString();
  }
}
