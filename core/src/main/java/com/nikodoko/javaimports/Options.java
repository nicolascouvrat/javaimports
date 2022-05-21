package com.nikodoko.javaimports;

import com.nikodoko.javaimports.common.metrics.MetricsConfiguration;
import com.nikodoko.javaimports.stdlib.StdlibProvider;
import com.nikodoko.javaimports.stdlib.StdlibProviders;
import java.nio.file.Path;
import java.util.Optional;
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
  /** Do not report metrics by default. */
  public static final MetricsConfiguration DEFAULT_METRICS_CONFIGURATION =
      MetricsConfiguration.disabled().build();
  /** Do not look for dependencies by default. */
  public static final Optional<Path> DEFAULT_REPOSITORY = Optional.empty();

  boolean debug;
  Optional<Path> repository;
  StdlibProvider stdlib;
  Executor executor;
  MetricsConfiguration metricsConfiguration;

  public Options(
      boolean debug,
      Optional<Path> repository,
      StdlibProvider stdlib,
      int numThreads,
      MetricsConfiguration metricsConfiguration) {
    this.debug = debug;
    this.repository = repository;
    this.stdlib = stdlib;
    this.executor = numThreads != 0 ? Executors.newFixedThreadPool(numThreads) : Runnable::run;
    this.metricsConfiguration = metricsConfiguration;
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

  /** The settings to use for metrics reporting */
  public MetricsConfiguration metricsConfiguration() {
    return metricsConfiguration;
  }

  public static class Builder {
    boolean debug = DEFAULT_IS_DEBUG;
    Optional<Path> repository = DEFAULT_REPOSITORY;
    StdlibProvider stdlib = DEFAULT_STDLIB_PROVIDER;
    int numThreads = DEFAULT_NUM_THREADS;
    MetricsConfiguration metricsConfiguration = DEFAULT_METRICS_CONFIGURATION;

    public Builder() {}

    public Builder debug(boolean debug) {
      this.debug = debug;
      return this;
    }

    public Builder repository(Path repository) {
      this.repository = Optional.of(repository);
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

    public Builder metricsConfiguration(MetricsConfiguration configuration) {
      this.metricsConfiguration = configuration;
      return this;
    }

    public Options build() {
      return new Options(debug, repository, stdlib, numThreads, metricsConfiguration);
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
