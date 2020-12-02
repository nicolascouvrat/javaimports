package com.nikodoko.javaimports.common;

import java.util.Objects;

public final class Identifier {
  private final String value;

  public Identifier(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof Identifier)) {
      return false;
    }

    var that = (Identifier) o;
    return Objects.equals(this.value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
