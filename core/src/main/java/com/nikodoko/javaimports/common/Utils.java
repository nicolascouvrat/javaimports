package com.nikodoko.javaimports.common;

import com.google.common.base.MoreObjects;

public class Utils {
  /** A wrapper around guava that should eventually be removed and replaced by custom code */
  public static class ToStringHelper {
    private MoreObjects.ToStringHelper helper;

    private <T> ToStringHelper(T obj) {
      this.helper = MoreObjects.toStringHelper(obj);
    }

    public ToStringHelper add(String propertyName, Object property) {
      helper.add(propertyName, property);
      return this;
    }

    @Override
    public String toString() {
      return helper.toString();
    }
  }

  public static <T> ToStringHelper toStringHelper(T obj) {
    return new ToStringHelper(obj);
  }
}
