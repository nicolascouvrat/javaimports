package com.nikodoko.javaimports.parser;

/** {@link Parser} options */
public class ParserOptions {
  boolean debug;

  public ParserOptions(boolean debug) {
    this.debug = debug;
  }

  /** Whether to run the {@code Parser} in debug mode */
  public boolean debug() {
    return debug;
  }

  public static class Builder {
    boolean debug;

    public Builder() {}

    public Builder debug(boolean debug) {
      this.debug = debug;
      return this;
    }

    public ParserOptions build() {
      return new ParserOptions(debug);
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
