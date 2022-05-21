package com.nikodoko.javaimports.common.metrics;

import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;

class DDAgentMetricsBackend implements MetricsBackend {
  private final StatsDClient client;

  DDAgentMetricsBackend(MetricsConfiguration config) {
    this.client =
        new NonBlockingStatsDClientBuilder()
            .hostname(config.datadogAgentHostname)
            .port(config.datadogAgentPort)
            // Enabling aggregation can cause an issue where metrics are not flushed, due to how
            // short-lived the javaimports process is.
            // Anyway, we're not going to send the same metric so many times that aggregation would
            // bring anything
            .enableAggregation(false)
            .build();
  }

  @Override
  public void count(String name, double value, String... tags) {
    client.count(name, value, tags);
  }
}
