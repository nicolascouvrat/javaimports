package com.nikodoko.javaimports;

import java.nio.file.Path;
import java.util.Optional;

/** {@link Importer} options */
public class Options {
  boolean debug;
  Optional<Path> repository;

  public Options(boolean debug, Optional<Path> repository) {
    this.debug = debug;
    this.repository = repository;
  }

  /** Specific directory to use as a dependency repository */
  public Optional<Path> repository() {
    return repository;
  }

  /** Whether to run the {@code Importer} in debug mode */
  public boolean debug() {
    return debug;
  }

  public static class Builder {
    boolean debug;
    Path repository;

    public Builder() {}

    public Builder debug(boolean debug) {
      this.debug = debug;
      return this;
    }

    public Builder repository(Path repository) {
      this.repository = repository;
      return this;
    }

    public Options build() {
      return new Options(debug, Optional.ofNullable(repository));
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
