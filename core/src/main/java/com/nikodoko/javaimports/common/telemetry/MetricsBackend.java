package com.nikodoko.javaimports.common.telemetry;

public interface MetricsBackend {
  public void count(String name, double value, String... tags);

  public void gauge(String name, double value, String... tags);
}
