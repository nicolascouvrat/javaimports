package com.nikodoko.javaimports.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@code Selector} describes a single java identifier or selector expression (list of identifiers
 * separated by a dot).
 */
public final class Selector {
  private final LinkedList<Identifier> identifiers;

  private Selector(Collection<Identifier> identifiers) {
    if (identifiers.isEmpty()) {
      throw new IllegalArgumentException(
          "cannot construct selector from an empty list of identifiers");
    }

    this.identifiers = new LinkedList<>(identifiers);
  }

  /** Converts a sequence of one or more strings to a {@code Selector}. */
  public static Selector of(String first, String... more) {
    var identifiers = new ArrayList<String>();
    identifiers.add(first);
    identifiers.addAll(Arrays.asList(more));
    return of(identifiers);
  }

  public static Selector of(Iterable<String> identifiers) {
    var l = new LinkedList<Identifier>();
    identifiers.forEach(s -> l.add(new Identifier(s)));
    return new Selector(l);
  }

  /** Returns the rightmost identifier of this {@code Selector}. */
  public Identifier identifier() {
    return identifiers.getLast();
  }

  public Optional<Selector> expression() {
    // TODO: implement
    return null;
  }

  /**
   * Constructs a new {@code Selector} by joining this selector to {@code other}.
   *
   * <p>This requires the last identifier of this selector to be equal to the first identifier of
   * the other, and the join will happen on that common identifier.
   *
   * <p>For example, if this selector represents {@code "a.b"}, then invoking this method with a
   * selector representing {@code "b.c"} will return a path representing {@code "a.b.c"}
   *
   * @param other the selector to join to this selector
   * @return the resulting selector
   * @exception IllegalArgumentException if {@code other} cannot be joined to this {@code Selector}
   */
  public Selector join(Selector other) {
    if (!other.identifiers.peekFirst().equals(identifiers.peekLast())) {
      throw new IllegalArgumentException("cannot join these selectors");
    }

    var combined =
        Stream.concat(identifiers.stream(), other.identifiers.stream().skip(1))
            .collect(Collectors.toList());

    return new Selector(combined);
  }

  public Selector subtract(Selector other) {
    // var minuend = identifiers.descendingIterator();
    // for (var identifier : other.identifiers.descendingIterator()) {
    //   if (!minuend.hasNext() || !minuend.next().equals(identifier)) {
    //     throw new IllegalArgumentException(
    //         String.format("%s cannot be subtracted from %s", other, this));
    //   }
    // }

    // var rest = new ArrayList<Identifier>();
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
