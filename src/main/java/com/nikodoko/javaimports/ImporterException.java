package com.nikodoko.javaimports;

import static java.util.Locale.ENGLISH;

import java.util.ArrayList;
import java.util.List;
import org.openjdk.javax.tools.Diagnostic;
import org.openjdk.javax.tools.JavaFileObject;

/** Exception class for importer errors */
public class ImporterException extends Exception {
  private List<ImporterDiagnostic> diagnostics;

  /**
   * Creates a new exception
   *
   * @param diagnostics a list of parser diagnostics
   */
  public static ImporterException fromDiagnostics(
      String filename, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
    List<ImporterDiagnostic> importerDiagnostics = new ArrayList<>();
    for (Diagnostic<?> d : diagnostics) {
      importerDiagnostics.add(ImporterDiagnostic.create(filename, d));
    }

    return new ImporterException(importerDiagnostics);
  }

  private ImporterException(List<ImporterDiagnostic> diagnostics) {
    this.diagnostics = diagnostics;
  }

  /**
   * Getter.
   *
   * @return the diagnostics
   */
  public List<ImporterDiagnostic> diagnostics() {
    return diagnostics;
  }

  /** Wrapper class for parser diagnostics */
  public static class ImporterDiagnostic {
    private final int line;
    private final int column;
    private final String message;
    private final String filename;

    /**
     * Wrap a parser diagnostic
     *
     * @param d the diagnostic to wrap
     */
    public static ImporterDiagnostic create(String filename, Diagnostic<?> d) {
      return new ImporterDiagnostic(
          filename, (int) d.getLineNumber(), (int) d.getColumnNumber(), d.getMessage(ENGLISH));
    }

    private ImporterDiagnostic(String filename, int line, int column, String message) {
      // TODO: assert > 0 with precondition
      this.filename = filename;
      this.line = line;
      this.column = column;
      this.message = message;
    }

    public String toString() {
      return filename + ":" + line + ":" + column + ": error: " + message;
    }
  }
}
