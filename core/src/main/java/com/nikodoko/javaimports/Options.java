package com.nikodoko.javaimports;

import com.nikodoko.javaimports.stdlib.StdlibProvider;
import com.nikodoko.javaimports.stdlib.StdlibProviders;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** {@link Importer} options */
public class Options {
  /** Do not use the stdlib by default. */
  public static final StdlibProvider DEFAULT_STDLIB_PROVIDER = StdlibProviders.empty();
  /** Do not use multithreading by default. */
  public static final int DEFAULT_NUM_THREADS = 0;
  /** Do not use debug logging by default. */
  public static final boolean DEFAULT_IS_DEBUG = false;
  /** Use local maven repository by default. */
  public static final Path DEFAULT_REPOSITORY =
      Paths.get(System.getProperty("user.home"), ".m2/repository");

  boolean debug;
  Path repository;
  StdlibProvider stdlib;
  Executor executor;

  public Options(boolean debug, Path repository, StdlibProvider stdlib, int numThreads) {
    this.debug = debug;
    this.repository = repository;
    this.stdlib = stdlib;
    this.executor = numThreads != 0 ? Executors.newFixedThreadPool(numThreads) : Runnable::run;
  }

  /** Specific directory to use as a dependency repository. */
  public Path repository() {
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
    boolean debug = DEFAULT_IS_DEBUG;
    Path repository = DEFAULT_REPOSITORY;
    StdlibProvider stdlib = DEFAULT_STDLIB_PROVIDER;
    int numThreads = DEFAULT_NUM_THREADS;

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
      return new Options(debug, repository, stdlib, numThreads);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Default options */
  public static Options defaults() {
    return builder().build();
  }
}
