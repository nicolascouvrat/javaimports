package com.nikodoko.javaimports.cli;

/** Command line options */
final class CLIOptions {
  private final String file;
  private final boolean help;
  private final boolean version;

  CLIOptions(String file, boolean help, boolean version) {
    this.file = file;
    this.help = help;
    this.version = version;
  }

  /** The file to operate on */
  String file() {
    return file;
  }

  /** Print usage informations */
  boolean help() {
    return help;
  }

  boolean version() {
    return version;
  }

  static class Builder {
    private String file;
    private boolean help;
    private boolean version;

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

    CLIOptions build() {
      return new CLIOptions(file, help, version);
    }
  }

  static Builder builder() {
    return new Builder();
  }
}
