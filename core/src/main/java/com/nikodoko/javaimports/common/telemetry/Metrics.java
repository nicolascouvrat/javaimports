package com.nikodoko.javaimports.common.telemetry;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Provides convenient access to metrics.
 *
 * <p>Make sure to use {@link #configure()} first.
 */
public class Metrics {
  private static final List<Tag> DEFAULT_TAGS = List.of(Tags.VERSION_TAG);
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

  private static Tag[] addDefaultTags(Tag... tags) {
    return Stream.concat(DEFAULT_TAGS.stream(), Arrays.stream(tags)).toArray(Tag[]::new);
  }

  public static void count(String name, double value, Tag... tags) {
    get().count(name, value, addDefaultTags(tags));
  }

  public static void gauge(String name, double value, Tag... tags) {
    get().gauge(name, value, addDefaultTags(tags));
  }
}
