package com.nikodoko.javaimports.fixer;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.parser.Import;
import java.util.HashSet;
import java.util.Set;

/** The result of a {@link Fixer} run. */
public class Result {
  private boolean done;
  private Set<Import> fixes = new HashSet<>();

  private Result(boolean done) {
    this.done = done;
  }

  private Result(boolean done, Set<Import> fixes) {
    this.done = done;
    this.fixes = fixes;
  }

  /**
   * A set of missing {@link Import} found by the {@code Fixer} and that have to be added to the
   * source file.
   */
  public Set<Import> fixes() {
    return fixes;
  }

  /**
   * Whether this {@code Result} is complete, or in other words, whether the {@code Fixer} managed
   * to find a matching import clause for all unresolved symbols.
   */
  public boolean done() {
    return done;
  }

  static Result incomplete(Set<Import> fixes) {
    return new Result(false, fixes);
  }

  static Result complete(Set<Import> fixes) {
    return new Result(true, fixes);
  }

  static Result complete() {
    return new Result(true);
  }

  static Result incomplete() {
    return new Result(false);
  }

  /** Debugging support. */
  public String toString() {
    return MoreObjects.toStringHelper(this).add("done", done).add("fixes", fixes).toString();
  }
  // by nikodoko.com
}
