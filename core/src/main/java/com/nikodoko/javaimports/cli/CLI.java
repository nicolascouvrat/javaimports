package com.nikodoko.javaimports.cli;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.nikodoko.javaimports.Importer;
import com.nikodoko.javaimports.ImporterException;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.stdlib.StdlibProviders;
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
  private final PrintWriter outWriter;

  private CLI(PrintWriter outWriter, PrintWriter errWriter) {
    this.errWriter = errWriter;
    this.outWriter = outWriter;
  }

  static String versionString() {
    return "javaimports: Version " + CLI.class.getPackage().getImplementationVersion();
  }

  /**
   * Main method
   *
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    int result;
    PrintWriter err = new PrintWriter(new OutputStreamWriter(System.err, UTF_8));
    PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, UTF_8));
    try {
      CLI parser = new CLI(out, err);
      result = parser.parse(args);
    } catch (UsageException e) {
      err.print(e.getMessage());
      result = 0;
    } finally {
      err.flush();
      out.flush();
    }

    System.exit(result);
  }

  private CLIOptions processArgs(String... args) throws UsageException {
    CLIOptions params;
    try {
      params = CLIOptionsParser.parse(Arrays.asList(args));
    } catch (IllegalArgumentException e) {
      throw new UsageException(e.getMessage());
    }

    if (params.file() == null && !(params.help() || params.version())) {
      throw new UsageException("please provide a file");
    }

    return params;
  }

  private String googleFormat(String code) {
    try {
      return new Formatter().formatSourceAndFixImports(code);
    } catch (FormatterException e) {
      // Formatting is not vital, so print a warning and continue
      errWriter.println("WARNING: formatter exception: " + e);
      errWriter.println("WARNING: output will not be formatted");
    }

    return code;
  }

  private int parse(String... args) throws UsageException {
    CLIOptions params = processArgs(args);

    if (params.version()) {
      errWriter.println(versionString());
      return 0;
    }

    if (params.help()) {
      throw new UsageException();
    }

    Path path;
    String input;
    try {
      // Importer expects an absolute path
      path = Paths.get(params.file()).toAbsolutePath();
      input = new String(Files.readAllBytes(path), UTF_8);
    } catch (IOException e) {
      errWriter.println(params.file() + ": could not read file: " + e.getMessage());
      return 1;
    }

    // TODO: make stdlib version a CLI option
    Options opts =
        Options.builder()
            .debug(params.verbose())
            .stdlib(StdlibProviders.java8())
            .numThreads(8)
            .build();
    String fixed;
    try {
      fixed = new Importer(opts).addUsedImports(path, input);
    } catch (ImporterException e) {
      for (ImporterException.ImporterDiagnostic d : e.diagnostics()) {
        errWriter.println(d);
      }

      return 1;
    }

    if (!params.fixOnly()) {
      fixed = googleFormat(fixed);
    }

    if (!params.replace()) {
      outWriter.write(fixed);
      return 0;
    }

    boolean changed = !fixed.equals(input);
    if (!changed) {
      // don't bother writing to file if nothing changed
      return 0;
    }

    try {
      Files.write(path, fixed.getBytes(UTF_8));
    } catch (IOException e) {
      errWriter.println(path + ": could not write file: " + e.getMessage());
      return 1;
    }

    return 0;
  }
}
