package com.nikodoko.javaimports.fixer.candidates;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.common.Import;
import java.util.Objects;

public final class Candidate {
  public static enum Source {
    SIBLING,
    STDLIB,
    EXTERNAL;
  }

  public final Import i;
  public final Source s;

  Candidate(Import i, Source s) {
    this.i = i;
    this.s = s;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof Candidate)) {
      return false;
    }

    var that = (Candidate) o;
    return Objects.equals(this.i, that.i) && Objects.equals(this.s, that.s);
  }

  @Override
  public int hashCode() {
    return Objects.hash(i, s);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("import", i).add("source", s).toString();
  }
}
