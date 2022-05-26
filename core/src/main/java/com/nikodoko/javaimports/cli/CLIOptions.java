package com.nikodoko.javaimports.cli;

/** Command line options */
final class CLIOptions {
  private final String file;
  private final boolean help;
  private final boolean version;
  private final boolean replace;
  private final boolean fixOnly;
  private final boolean verbose;
  private final String assumeFilename;
  private final boolean metricsEnabled;
  // These two options are used only if metrics are enabled
  private final Integer metricsDatadogPort;
  private final String metricsDatadogHost;
  private final boolean tracingEnabled;

  CLIOptions(
      String file,
      boolean help,
      boolean version,
      boolean replace,
      boolean fixOnly,
      boolean verbose,
      String assumeFilename,
      boolean metricsEnabled,
      Integer metricsDatadogPort,
      String metricsDatadogHost,
      boolean tracingEnabled) {
    this.file = file;
    this.help = help;
    this.version = version;
    this.replace = replace;
    this.fixOnly = fixOnly;
    this.verbose = verbose;
    this.assumeFilename = assumeFilename;
    this.metricsEnabled = metricsEnabled;
    this.metricsDatadogPort = metricsDatadogPort;
    this.metricsDatadogHost = metricsDatadogHost;
    this.tracingEnabled = tracingEnabled;
  }

  /** The file to operate on */
  String file() {
    return file;
  }

  /** Print usage informations */
  boolean help() {
    return help;
  }

  /** Whether to operate in place (and write to source file) */
  boolean replace() {
    return replace;
  }

  /** If true, no formatting should be done outside of adding and removing imports */
  boolean fixOnly() {
    return fixOnly;
  }

  /** If true, output debug information */
  boolean verbose() {
    return verbose;
  }

  boolean version() {
    return version;
  }

  /** File name to use for diagnostics when parsing standard input. */
  String assumeFilename() {
    return assumeFilename;
  }

  /** Whether to enable metrics reporting */
  boolean metricsEnabled() {
    return metricsEnabled;
  }

  /** The port to use when reporting metrics to the datadog agent. */
  Integer metricsDatadogPort() {
    return metricsDatadogPort;
  }

  /** The hostname to use when reporting metrics to the datadog agent. */
  String metricsDatadogHost() {
    return metricsDatadogHost;
  }

  boolean tracingEnabled() {
    return tracingEnabled;
  }

  static class Builder {
    private String file;
    private boolean help;
    private boolean version;
    private boolean replace;
    private boolean fixOnly;
    private boolean verbose;
    private String assumeFilename;
    private boolean metricsEnabled;
    private Integer metricsDatadogPort;
    private String metricsDatadogHost;
    private boolean tracingEnabled;

    Builder file(String file) {
      this.file = file;
      return this;
    }

    Builder help(boolean help) {
      this.help = help;
      return this;
    }

    Builder version(boolean version) {
      this.version = version;
      return this;
    }

    Builder replace(boolean replace) {
      this.replace = replace;
      return this;
    }

    Builder fixOnly(boolean fixOnly) {
      this.fixOnly = fixOnly;
      return this;
    }

    Builder verbose(boolean verbose) {
      this.verbose = verbose;
      return this;
    }

    Builder assumeFilename(String assumeFilename) {
      this.assumeFilename = assumeFilename;
      return this;
    }

    Builder metricsEnabled(boolean metricsEnabled) {
      this.metricsEnabled = metricsEnabled;
      return this;
    }

    Builder metricsDatadogPort(int port) {
      this.metricsDatadogPort = port;
      return this;
    }

    Builder metricsDatadogHost(String host) {
      this.metricsDatadogHost = host;
      return this;
    }

    Builder tracingEnabled(boolean tracingEnabled) {
      this.tracingEnabled = tracingEnabled;
      return this;
    }

    CLIOptions build() {
      return new CLIOptions(
          file,
          help,
          version,
          replace,
          fixOnly,
          verbose,
          assumeFilename,
          metricsEnabled,
          metricsDatadogPort,
          metricsDatadogHost,
          tracingEnabled);
    }
  }

  static Builder builder() {
    return new Builder();
  }
}
