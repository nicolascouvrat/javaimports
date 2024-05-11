package com.nikodoko.javaimports.common;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

  public static void checkNotNull(Object o, String errorMsg) {
    assert o != null : errorMsg;
  }

  public static void checkNotNull(Object o) {
    assert o != null;
  }

  public static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> tasks) {
    return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
        .thenApply(__ -> tasks.stream().map(CompletableFuture::join).toList());
  }
}
