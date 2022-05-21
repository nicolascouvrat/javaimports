package com.nikodoko.javaimports.common.metrics;

class NoopMetricsBackend implements MetricsBackend {
  @Override
  public void count(String name, double value, String... tags) {}

  @Override
  public void gauge(String name, double value, String... tags) {}
}
