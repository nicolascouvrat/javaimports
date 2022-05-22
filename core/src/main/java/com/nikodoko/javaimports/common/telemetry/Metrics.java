package com.nikodoko.javaimports.common.telemetry;

/**
 * Provides convenient access to metrics.
 *
 * <p>Make sure to use {@link #configure()} first.
 */
public class Metrics {
  private static volatile MetricsBackend backend;

  public static void configure(MetricsConfiguration config) {
    if (backend != null) {
      return;
    }

    backend = createBackend(config);
  }

  private static synchronized MetricsBackend createBackend(MetricsConfiguration config) {
    if (backend != null) {
      return backend;
    }

    if (config.enabled) {
      return new DDAgentMetricsBackend(config);
    }

    return NoopMetricsBackend.INSTANCE;
  }

  private static MetricsBackend get() {
    if (backend == null) {
      // Drop the metric
      return NoopMetricsBackend.INSTANCE;
    }

    return backend;
  }

  public static void count(String name, double value, String... tags) {
    get().count(name, value, tags);
  }

  public static void gauge(String name, double value, String... tags) {
    get().gauge(name, value, tags);
  }
}
