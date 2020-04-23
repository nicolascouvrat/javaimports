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
    "  --replace, -replace, -r, -w",
    "    Write result to source file instead of stdout.",
    "  --version, -version, -v",
    "    Print the version.",
    "  --help, -help, -h",
    "    Print this usage statement.",
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
