package com.nikodoko.javaimports.cli;

/** Command line options */
final class CLIOptions {
  private final String file;
  private final boolean help;
  private final boolean version;
  private final boolean replace;

  CLIOptions(String file, boolean help, boolean version, boolean replace) {
    this.file = file;
    this.help = help;
    this.version = version;
    this.replace = replace;
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

  boolean version() {
    return version;
  }

  static class Builder {
    private String file;
    private boolean help;
    private boolean version;
    private boolean replace;

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

    CLIOptions build() {
      return new CLIOptions(file, help, version, replace);
    }
  }

  static Builder builder() {
    return new Builder();
  }
}
