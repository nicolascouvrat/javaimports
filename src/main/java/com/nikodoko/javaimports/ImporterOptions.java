package com.nikodoko.javaimports;

import java.util.List;

/** {@link Importer} options */
public class ImporterOptions {
  boolean debug;
  List<String> test;

  public ImporterOptions(boolean debug) {
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

    public ImporterOptions build() {
      return new ImporterOptions(debug);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Default options */
  public static ImporterOptions defaults() {
    return builder().debug(false).build();
  }
}
