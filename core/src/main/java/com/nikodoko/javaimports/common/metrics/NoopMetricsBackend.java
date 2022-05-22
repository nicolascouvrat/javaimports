package com.nikodoko.javaimports.common.metrics;

class NoopMetricsBackend implements MetricsBackend {
  public static NoopMetricsBackend INSTANCE = new NoopMetricsBackend();

  @Override
  public void count(String name, double value, String... tags) {}

  @Override
  public void gauge(String name, double value, String... tags) {}
}
