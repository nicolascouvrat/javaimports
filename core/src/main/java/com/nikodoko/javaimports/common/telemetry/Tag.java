package com.nikodoko.javaimports.common.telemetry;

/** Simple wrapper to unify tags accross traces and metrics. */
public class Tag<T> {
  private static final String DELIMITER = ":";

  final String key;
  final String value;

  public Tag(String key, T value) {
    this.key = key;
    this.value = asString(value);
  }

  private String asString(T value) {
    if (value == null) {
      return "";
    }

    return value.toString();
  }

  public static <T> Key<T> withKey(String key) {
    return new Key<T>(key);
  }

  @Override
  public String toString() {
    return String.join(DELIMITER, key, value);
  }

  public static class Key<T> {
    private final String key;

    Key(String key) {
      this.key = key;
    }

    public Tag<T> is(T value) {
      return new Tag(key, value);
    }
  }
}
