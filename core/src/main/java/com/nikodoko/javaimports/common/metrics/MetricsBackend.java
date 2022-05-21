package com.nikodoko.javaimports.common.metrics;

public interface MetricsBackend {
  public void count(String name, double value, String... tags);
}
