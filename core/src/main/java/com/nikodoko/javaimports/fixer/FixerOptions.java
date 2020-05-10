package com.nikodoko.javaimports.fixer;

/** {@link Fixer} options */
public class FixerOptions {
  boolean debug;

  public FixerOptions(boolean debug) {
    this.debug = debug;
  }

  /** Whether to run the {@code Fixer} in debug mode */
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

    public FixerOptions build() {
      return new FixerOptions(debug);
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
