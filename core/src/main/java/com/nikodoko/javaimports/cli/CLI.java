package com.nikodoko.javaimports.cli;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.nikodoko.javaimports.Importer;
import com.nikodoko.javaimports.ImporterException;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.common.telemetry.Metrics;
import com.nikodoko.javaimports.common.telemetry.MetricsConfiguration;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.stdlib.StdlibProviders;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
      CLI cli = new CLI(out, err);
      CLIOptions params = processArgs(args);
      result = cli.run(params);
    } catch (UsageException e) {
      err.print(e.getMessage());
      result = 0;
    } finally {
      err.flush();
      out.flush();
    }

    System.exit(result);
  }

  private static CLIOptions processArgs(String... args) throws UsageException {
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

  private String readStdin() throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String line = "", file = "";
    while ((line = br.readLine()) != null) {
      file = file + line + "\n";
    }
    return file;
  }

  private void instrument(CLIOptions params) {
    // Build metrics configuration
    var metricsConfig = MetricsConfiguration.disabled().build();
    if (params.metricsEnabled()) {
      var metricsConfigBuilder = MetricsConfiguration.enabled();
      if (params.metricsDatadogPort() != null) {
        metricsConfigBuilder.datadogAgentPort(params.metricsDatadogPort());
      }

      if (params.metricsDatadogHost() != null) {
        metricsConfigBuilder.datadogAgentHostname(params.metricsDatadogHost());
      }

      metricsConfig = metricsConfigBuilder.build();
    }

    Metrics.configure(metricsConfig);
    if (params.tracingEnabled()) {
      Traces.enable();
    }
  }

  private int run(CLIOptions params) throws UsageException {
    instrument(params);
    var span = Traces.createSpan("CLI.run");
    try (var __ = Traces.activate(span)) {
      return runInstrumented(params);
    } finally {
      span.finish();
      Traces.close();
    }
  }

  private int runInstrumented(CLIOptions params) throws UsageException {
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
      if (params.file().equals("-")) { // Read from Stdin
        var f = params.assumeFilename();
        if (f == null) {
          f = ""; // assumes current working directory if assume-filename isn't specified
        }
        path = Paths.get(f).toAbsolutePath();
        input = readStdin();
      } else {
        // Importer expects an absolute path
        path = Paths.get(params.file()).toAbsolutePath();
        input = new String(Files.readAllBytes(path), UTF_8);
      }

    } catch (IOException e) {
      errWriter.println(params.file() + ": could not read file: " + e.getMessage());
      return 1;
    }

    // TODO: make stdlib version a CLI option
    // TODO: use number of threads according to processor
    var optsBuilder =
        Options.builder().debug(params.verbose()).stdlib(StdlibProviders.java8()).numThreads(8);
    if (params.repository() != null) {
      optsBuilder.repository(Paths.get(params.repository()));
    }

    if (params.verbose()) {
      Logs.enable();
    }

    String fixed;
    try {
      fixed = new Importer(optsBuilder.build()).addUsedImports(path, input);
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
