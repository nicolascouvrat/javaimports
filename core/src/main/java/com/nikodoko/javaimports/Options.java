package com.nikodoko.javaimports;

import com.nikodoko.javaimports.stdlib.StdlibProvider;
import com.nikodoko.javaimports.stdlib.StdlibProviders;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** {@link Importer} options */
public class Options {
  boolean debug;
  Optional<Path> repository;
  StdlibProvider stdlib;
  Executor executor;

  public Options(boolean debug, Optional<Path> repository, StdlibProvider stdlib, int numThreads) {
    this.debug = debug;
    this.repository = repository;
    this.stdlib = stdlib;
    this.executor = numThreads != 0 ? Executors.newFixedThreadPool(numThreads) : Runnable::run;
  }

  /** Specific directory to use as a dependency repository. */
  public Optional<Path> repository() {
    return repository;
  }

  /** Whether to run the {@code Importer} in debug mode. */
  public boolean debug() {
    return debug;
  }

  /** The standard library to use for this run. */
  public StdlibProvider stdlib() {
    return stdlib;
  }

  /** The executor to use to run parallel tasks */
  public Executor executor() {
    return executor;
  }

  public static class Builder {
    boolean debug;
    Path repository;
    StdlibProvider stdlib;
    int numThreads;

    public Builder() {}

    public Builder debug(boolean debug) {
      this.debug = debug;
      return this;
    }

    public Builder repository(Path repository) {
      this.repository = repository;
      return this;
    }

    public Builder stdlib(StdlibProvider stdlib) {
      this.stdlib = stdlib;
      return this;
    }

    public Builder numThreads(int numThreads) {
      this.numThreads = numThreads;
      return this;
    }

    public Options build() {
      return new Options(debug, Optional.ofNullable(repository), stdlib, numThreads);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Default options */
  public static Options defaults() {
    return builder().debug(false).stdlib(StdlibProviders.empty()).numThreads(0).build();
  }
}
