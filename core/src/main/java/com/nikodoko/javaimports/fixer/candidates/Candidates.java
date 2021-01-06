package com.nikodoko.javaimports.fixer.candidates;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.common.Selector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Candidates {
  static final Candidates EMPTY = new Candidates(Map.of());

  private final Map<Selector, List<Candidate>> candidates;

  private Candidates(Map<Selector, List<Candidate>> candidates) {
    this.candidates = candidates;
  }

  /**
   * Returns a list of {@link Candidate} for this {@code selector}, or an empty list if none are
   * present.
   */
  List<Candidate> getFor(Selector selector) {
    return candidates.getOrDefault(selector, new ArrayList<>());
  }

  /**
   * Returns all the selectors for which this {@code Candidates} contains one or more {@link
   * Candidate}.
   */
  Set<Selector> selectors() {
    return candidates.keySet();
  }

  /** Returns true if this {@code Candidates} does not contain any candidates. */
  boolean isEmpty() {
    return candidates.isEmpty();
  }

  static Builder forSelector(Selector s) {
    return new Builder(s);
  }

  /**
   * Creates a new instance of {@code Candidates} containing all the candidates stored in {@code a}
   * and {@code b}.
   */
  public static Candidates merge(Candidates a, Candidates b) {
    var combined = new HashMap<Selector, List<Candidate>>();
    combined.putAll(a.candidates);
    combined.putAll(b.candidates);
    return new Candidates(combined);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof Candidates)) {
      return false;
    }

    var that = (Candidates) o;
    return Objects.equals(this.candidates, that.candidates);
  }

  @Override
  public int hashCode() {
    return Objects.hash(candidates);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("candidates", candidates).toString();
  }

  static class Builder {
    private final Selector selector;
    private List<Candidate> candidates = new ArrayList<>();

    private Builder(Selector selector) {
      this.selector = selector;
    }

    Builder add(Collection<Candidate> candidates) {
      this.candidates.addAll(candidates);
      return this;
    }

    Builder add(Candidate... candidates) {
      Collections.addAll(this.candidates, candidates);
      return this;
    }

    Candidates build() {
      return new Candidates(Map.of(selector, candidates));
    }
  }
}
