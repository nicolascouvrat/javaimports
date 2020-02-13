package com.nikodoko.importer;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/** The main class for the CLI */
public final class CLI {
  private final PrintWriter errWriter;

  private CLI(PrintWriter errWriter) {
    this.errWriter = errWriter;
  }

  /**
   * Main method
   *
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    PrintWriter err = new PrintWriter(new OutputStreamWriter(System.err, UTF_8));

    CLI parser = new CLI(err);
    int result = parser.parse(args);

    // Print all errors
    err.flush();
    System.exit(result);
  }

  public int parse(String... args) {
    CLIOptions params = CLIOptionsParser.parse(Arrays.asList(args));
    System.out.println(params.file());

    Path path = Paths.get(params.file());
    String input;
    try {
      input = new String(Files.readAllBytes(path), UTF_8);
      System.out.println(input);
    } catch (IOException e) {
      errWriter.println(params.file() + ": could not read file: " + e.getMessage());
      return 1;
    }

    return 0;
  }
}
