package com.nikodoko.javaimports.common;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@code Selector} describes a single java identifier or selector expression (list of identifiers
 * separated by a dot).
 */
public final class Selector {
  /**
   * Calculates the distance between two {@link Selector}.
   *
   * <p>That distance is to be understood as the length of the relative path from one selector to
   * the other.
   *
   * <p>For example, the selector representing {@code com.a.package} and the one representing {@code
   * com.a.package.subpackage} have a distance of 1, {@code com.a.package} and {@code com.a} also
   * have a distance of 1, and {@code com.a.package} and {@code net.another.package} have a distance
   * of 6.
   */
  public static final class Distance {
    private final Path reference;

    private Distance(Path reference) {
      this.reference = reference;
    }

    public static Distance from(Selector s) {
      return new Distance(asPath(s));
    }

    private static Path asPath(Selector s) {
      var referencePath =
          s.identifiers.stream().map(Identifier::toString).collect(Collectors.joining("/"));
      return Paths.get(referencePath);
    }

    public int to(Selector s) {
      return distance(reference, asPath(s));
    }

    private int distance(Path from, Path to) {
      if (from.equals(to)) {
        return 0;
      }

      return from.relativize(to).getNameCount();
    }
  }

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

  public static Selector of(Identifier identifier) {
    return new Selector(List.of(identifier));
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

  /**
   * Returns a {@code Selector} representing the scope in which the rightmos identifier of this
   * {@code Selector} is.
   */
  public Selector scope() {
    return new Selector(identifiers.subList(0, identifiers.size() - 1));
  }

  // TODO: tentative API, used only in test, should maybe be removed
  public Selector combine(Selector other) {
    var combined =
        Stream.concat(identifiers.stream(), other.identifiers.stream())
            .collect(Collectors.toList());

    return new Selector(combined);
  }

  /**
   * Returns the number of identifiers the selector expression represented by this {@code Selector}
   * contains.
   */
  public int size() {
    return identifiers.size();
  }

  /**
   * Constructs a new {@code Selector} by joining this selector to {@code other}.
   *
   * <p>This requires the last identifier of this selector to be equal to the first identifier of
   * the other, and the join will happen on that common identifier.
   *
   * <p>For example, if this selector represents {@code "a.b"}, then invoking this method with a
   * selector representing {@code "b.c"} will return a path representing {@code "a.b.c"}.
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

  /**
   * Constructs a new {@code Selector} by subtracting {@code other} from this selector.
   *
   * <p>This operation is the opposite of {@link #join(Selector)}, so {@code
   * selector.join(other).subtract(other).equals(selector) == true}.
   *
   * <p>For example, if this selector represents {@code "a.b.c"}, then invoking this method with a
   * selector representing {@code "b.c"} will return a path representing {@code "a.b"}.
   *
   * @param other the selector to subtract to this selector
   * @return the resulting selector
   * @exception IllegalArgumentException if {@code other} cannot be subtracted from this {@code
   *     Selector}
   */
  public Selector subtract(Selector other) {
    if (!endsWith(other)) {
      throw new IllegalArgumentException(
          String.format("%s cannot be subtracted from %s", other, this));
    }

    // The +1 is to keep the last common identifier
    var cutoff = identifiers.size() - other.identifiers.size() + 1;
    return new Selector(List.copyOf(identifiers.subList(0, cutoff)));
  }

  /**
   * Constructs a new {@code Selector} by rebasing this selector on a {@code base}.
   *
   * <p>This requires that this selector {@link #startsWith} {@code base}, and will trim {@code
   * base} off this selector. It will fail if {@code base.equals(this)} is {@code true}.
   *
   * <p>For example, if this selector represents {@code "a.b.c.d"}, then invoking this method with a
   * selector representing {@code a.b} will return a selector representing {@code "c.d"}.
   *
   * @param base the selector to rebase this selector on
   * @return the resulting selector
   * @exception IllegalArgumentException if this selector does not start with {@code base}, or if
   *     {@code base} is equal to this selector
   */
  public Selector rebase(Selector base) {
    if (!startsWith(base) || equals(base)) {
      throw new IllegalArgumentException(String.format("Cannot rebase %s on %s", this, base));
    }

    return new Selector(List.copyOf(identifiers.subList(base.size(), identifiers.size())));
  }

  /** Returns true if this {@code Selector} ends with {@code other}. */
  public boolean endsWith(Selector other) {
    var thisLength = identifiers.size();
    var otherLength = other.identifiers.size();
    if (otherLength > thisLength) {
      return false;
    }

    return identifiers.subList(thisLength - otherLength, thisLength).equals(other.identifiers);
  }

  /** Returns true if this {@code Selector} starts wit {@code other}. */
  public boolean startsWith(Selector other) {
    var thisLength = identifiers.size();
    var otherLength = other.identifiers.size();
    if (otherLength > thisLength) {
      return false;
    }

    return identifiers.subList(0, otherLength).equals(other.identifiers);
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
