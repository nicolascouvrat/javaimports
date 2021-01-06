package com.nikodoko.javaimports.common;

import com.google.common.base.MoreObjects;
import java.util.Objects;

public final class Import {
  public final Selector selector;
  public final boolean isStatic;

  public Import(Selector selector, boolean isStatic) {
    this.selector = selector;
    this.isStatic = isStatic;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof Import)) {
      return false;
    }

    var that = (Import) o;
    return Objects.equals(that.selector, this.selector) && this.isStatic == that.isStatic;
  }

  @Override
  public int hashCode() {
    return Objects.hash(selector, isStatic);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("selector", selector)
        .add("isStatic", isStatic)
        .toString();
  }
}
