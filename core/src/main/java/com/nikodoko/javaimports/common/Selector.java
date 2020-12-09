package com.nikodoko.javaimports.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A {@code Selector} describes a single java identifier or selector expression (list of identifiers
 * separated by a dot).
 */
public final class Selector {
  private final LinkedList<Identifier> identifiers;

  private Selector(Collection<Identifier> identifiers) {
    // TODO: assert that the size is at least one
    this.identifiers = new LinkedList<>(identifiers);
  }

  public static Selector of(String... identifiers) {
    return of(Arrays.asList(identifiers));
  }

  public static Selector of(Iterable<String> identifiers) {
    var l = new LinkedList<Identifier>();
    identifiers.forEach(s -> l.add(new Identifier(s)));
    return new Selector(l);
  }

  public Identifier identifier() {
    return identifiers.getLast();
  }

  public Optional<Selector> expression() {
    // TODO: implement
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {

      return false;
    }

    if (!(o instanceof Selector)) {
      return false;
    }

    var that = (Selector) o;
    return Objects.equals(this.identifiers, that.identifiers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifiers);
  }

  @Override
  public String toString() {
    return identifiers.stream().map(Identifier::toString).collect(Collectors.joining("."));
  }
}
