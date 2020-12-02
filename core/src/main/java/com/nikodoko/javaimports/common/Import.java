package com.nikodoko.javaimports.common;

import com.google.common.base.MoreObjects;
import java.util.Objects;

public final class Import {
  private final String pkg;
  private final Selector selector;
  private final boolean isStatic;

  public Import(String pkg, Selector selector, boolean isStatic) {
    this.pkg = pkg;
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
    return Objects.equals(that.pkg, this.pkg)
        && Objects.equals(that.selector, this.selector)
        && this.isStatic == that.isStatic;
  }

  @Override
  public int hashCode() {
    return Objects.hash(pkg, selector, isStatic);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("pkg", pkg)
        .add("selector", selector)
        .add("isStatic", isStatic)
        .toString();
  }
}
