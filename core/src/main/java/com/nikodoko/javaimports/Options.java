package com.nikodoko.javaimports;

/** {@link Importer} options */
public class Options {
  boolean debug;

  public Options(boolean debug) {
    this.debug = debug;
  }

  /** Whether to run the {@code Importer} in debug mode */
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

    public Options build() {
      return new Options(debug);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Default options */
  public static Options defaults() {
    return builder().debug(false).build();
  }
}
