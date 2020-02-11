package com.nikodoko.importer;

import java.util.Arrays;

/** The main class for the CLI */
public final class Main {
  private Main() {}

  public static void main(String[] args) {
    Main parser = new Main();
    int result = parser.parse(args);

    System.exit(result);
  }

  public int parse(String... args) {
    CLIOptions params = CLIOptionsParser.parse(Arrays.asList(args));
    System.out.println(params.file());
    return 0;
  }
}
