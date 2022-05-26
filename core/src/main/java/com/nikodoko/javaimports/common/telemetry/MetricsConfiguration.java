package com.nikodoko.javaimports.common.telemetry;

public class MetricsConfiguration {
  private static final int DEFAULT_DD_PORT = 8125;
  private static final String DEFAULT_DD_HOST = "localhost";

  final boolean enabled;
  final int datadogAgentPort;
  final String datadogAgentHostname;

  private MetricsConfiguration(boolean enabled, String datadogAgentHostname, int datadogAgentPort) {
    this.enabled = enabled;
    this.datadogAgentHostname = datadogAgentHostname;
    this.datadogAgentPort = datadogAgentPort;
  }

  public static Builder enabled() {
    return new Builder(true);
  }

  public static Builder disabled() {
    return new Builder(false);
  }

  public static class Builder {
    final boolean enabled;
    int datadogAgentPort = DEFAULT_DD_PORT;
    String datadogAgentHostname = DEFAULT_DD_HOST;

    public Builder(boolean enabled) {
      this.enabled = enabled;
    }

    public Builder datadogAgentPort(int port) {
      this.datadogAgentPort = port;
      return this;
    }

    public Builder datadogAgentHostname(String host) {
      this.datadogAgentHostname = host;
      return this;
    }

    public MetricsConfiguration build() {
      return new MetricsConfiguration(enabled, datadogAgentHostname, datadogAgentPort);
    }
  }
}
