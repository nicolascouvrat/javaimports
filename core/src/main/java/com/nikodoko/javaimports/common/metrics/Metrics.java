package com.nikodoko.javaimports.common.metrics;

/**
 * Provides convenient access to metrics.
 *
 * <p>Make sure to use {@link #configure()} first.
 */
public class Metrics {
  private static volatile MetricsBackend backend;

  public static void configure() {
    if (backend != null) {
      return;
    }

    backend = createBackend();
  }

  private static synchronized MetricsBackend createBackend() {
    if (backend != null) {
      return backend;
    }

    return new DDAgentMetricsBackend();
  }

  private static MetricsBackend get() {
    if (backend == null) {
      throw new IllegalStateException("Metrics backend is not configured!");
    }

    return backend;
  }

  public static void count(String name, double value, String... tags) {
    get().count(name, value, tags);
  }
}
