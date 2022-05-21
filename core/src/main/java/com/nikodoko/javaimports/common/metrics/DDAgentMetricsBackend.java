package com.nikodoko.javaimports.common.metrics;

import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;

class DDAgentMetricsBackend implements MetricsBackend {
  private final StatsDClient client;

  DDAgentMetricsBackend() {
    this.client =
        new NonBlockingStatsDClientBuilder()
            .hostname("localhost")
            .port(8125)
            .enableAggregation(false)
            .build();
  }

  @Override
  public void count(String name, double value, String... tags) {
    client.count(name, value, tags);
  }
}
