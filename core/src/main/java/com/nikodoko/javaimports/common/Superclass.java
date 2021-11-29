package com.nikodoko.javaimports.common;

import java.util.Objects;
import java.util.Optional;

/**
 * A pointer to a superclass.
 *
 * <p>It can either be resolved (and contain an {@link Import}) or not (and contain a {@link
 * Selector}).
 */
public final class Superclass {
  private final Optional<Selector> maybeSelector;
  private final Optional<Import> maybeImport;

  private Superclass(Optional<Selector> maybeSelector, Optional<Import> maybeImport) {
    if (maybeSelector.isPresent() == maybeImport.isPresent()) {
      throw new IllegalArgumentException("A superclass should have a selector or an import");
    }

    this.maybeImport = maybeImport;
    this.maybeSelector = maybeSelector;
  }

  public static Superclass resolved(Import superclass) {
    return new Superclass(Optional.empty(), Optional.of(superclass));
  }

  public static Superclass unresolved(Selector selector) {
    return new Superclass(Optional.of(selector), Optional.empty());
  }

  public Import getResolved() {
    if (!isResolved()) {
      throw new IllegalStateException(
          "Attempting to get the resolved superclass but this superclass is not resolved: " + this);
    }

    return maybeImport.get();
  }

  public Selector getUnresolved() {
    // XXX: maybe this should throw instead? but technically it is possible to get the unresolved
    // class when we have the resolved one available
    if (isResolved()) {
      return maybeImport.get().selector;
    }

    return maybeSelector.get();
  }

  public boolean isResolved() {
    return maybeImport.isPresent();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof Superclass)) {
      return false;
    }

    var that = (Superclass) o;
    return Objects.equals(this.maybeSelector, that.maybeSelector)
        && Objects.equals(this.maybeImport, that.maybeImport);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maybeSelector, maybeImport);
  }

  @Override
  public String toString() {
    return Utils.toStringHelper(this)
        .add("maybeSelector", maybeSelector)
        .add("maybeImport", maybeImport)
        .toString();
  }
}
