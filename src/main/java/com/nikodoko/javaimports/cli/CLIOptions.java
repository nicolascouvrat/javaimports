package com.nikodoko.javaimports.cli;

/** Command line options */
final class CLIOptions {
  private final String file;

  CLIOptions(String file) {
    this.file = file;
  }

  /** The file to operate on */
  String file() {
    return file;
  }

  static class Builder {
    private String file;

    Builder file(String file) {
      this.file = file;
      return this;
    }

    CLIOptions build() {
      return new CLIOptions(file);
    }
  }

  static Builder builder() {
    return new Builder();
  }
}
