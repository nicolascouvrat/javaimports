package com.nikodoko.javaimports.cli;

import com.google.common.base.Joiner;

/** Exception class for CLI usage errors */
public class UsageException extends Exception {
  private static final Joiner NEWLINE_JOINER = Joiner.on(System.lineSeparator());

  private static final String[] USAGE = {
    "",
    "Usage: javaimports [options] file",
    "",
    "Options:",
    "  --assume-filename, -assume-filename",
    "    File name to use for diagnostics when importing standard input (default is .).",
    "  --fix-only",
    "    Do not format ouput, simply add and remove imports.",
    "  --metrics-datadog-port, -metrics-datadog-port",
    "    Port to use when --metrics-enable is set (default is 8125).",
    "  --metrics-datadog-host, -metrics-datadog-host",
    "    Host to use when --metrics-enable is set (default is \"localhost\").",
    "  --metrics-enable, -metrics-enable",
    "    Enable metrics reporting to a datadog agent running on the specified port and host.",
    "  --replace, -replace, -r, -w",
    "    Write result to source file instead of stdout.",
    "  --telemetry-enable, -telemetry-enable",
    "    Enable telemetry. Shorthand for --tracing-enable and --metrics-enable.",
    "  --tracing-enable, -tracing-enable",
    "    Enable tracing reporting to a datadog agent listening at http://localhost:8126.",
    "  --verbose, -verbose, -v",
    "    Verbose logging.",
    "  --version, -version",
    "    Print the version.",
    "  --help, -help, -h",
    "    Print this usage statement.",
    "",
    "File:",
    "  setting file equal to '-' will read from stdin",
    "",
  };

  public UsageException(String message) {
    super(build(message));
  }

  public UsageException() {
    super(build(null));
  }

  private static String build(String message) {
    StringBuilder builder = new StringBuilder();
    if (message != null) {
      appendLine(builder, message);
    }

    appendUsage(builder);
    appendLine(builder, "");
    appendLine(builder, CLI.versionString());
    return builder.toString();
  }

  private static void appendLine(StringBuilder builder, String line) {
    builder.append(line).append(System.lineSeparator());
  }

  private static void appendUsage(StringBuilder builder) {
    NEWLINE_JOINER.appendTo(builder, USAGE).append(System.lineSeparator());
  }
}
