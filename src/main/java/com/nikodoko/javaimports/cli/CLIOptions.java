package com.nikodoko.javaimports.cli;

/** Command line options */
final class CLIOptions {
  private final String file;
  private final boolean help;
  private final boolean version;
  private final boolean replace;
  private final boolean fixOnly;
  private final boolean verbose;

  CLIOptions(
      String file,
      boolean help,
      boolean version,
      boolean replace,
      boolean fixOnly,
      boolean verbose) {
    this.file = file;
    this.help = help;
    this.version = version;
    this.replace = replace;
    this.fixOnly = fixOnly;
    this.verbose = verbose;
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

  static class Builder {
    private String file;
    private boolean help;
    private boolean version;
    private boolean replace;
    private boolean fixOnly;
    private boolean verbose;

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

    CLIOptions build() {
      return new CLIOptions(file, help, version, replace, fixOnly, verbose);
    }
  }

  static Builder builder() {
    return new Builder();
  }
}
