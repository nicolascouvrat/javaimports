package com.nikodoko.javaimports;

public class ImporterOptions {
  boolean debug;

  public ImporterOptions(boolean debug) {
    this.debug = debug;
  }

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
}
