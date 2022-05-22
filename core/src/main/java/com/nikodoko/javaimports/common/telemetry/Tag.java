package com.nikodoko.javaimports.common.telemetry;

/** Simple wrapper to unify tags accross traces and metrics. */
public class Tag {
  private static final String DELIMITER = ":";

  final String key;
  final String value;

  public Tag(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public static Key withKey(String key) {
    return new Key(key);
  }

  @Override
  public String toString() {
    return String.join(DELIMITER, key, value);
  }

  public static class Key {
    private final String key;

    Key(String key) {
      this.key = key;
    }

    public Tag is(String value) {
      return new Tag(key, value);
    }
  }
}
