package com.nikodoko.javaimports.cli;

import java.util.Iterator;

public class CLIOptionsParser {
  /** Holds a flag and its (optional) value. */
  private static class FlagAndValue {
    private static char SEPARATOR = '=';

    /** The flag, for example "--help" */
    public String flag;
    /** The value, optionally null */
    public String value;

    private FlagAndValue(String flag, String value) {
      this.flag = flag;
      this.value = value;
    }

    /**
     * Create a {@code FlagAndValue} from a string. If s contains an {@code '='}, then {@code flag}
     * will be whatever is before and value whatever is after. Else, {@code flag} will be equal to
     * {@code s} and {@code value} will be {@code null}.
     */
    public static FlagAndValue fromString(String s) {
      int idx = s.indexOf(SEPARATOR);
      if (idx < 0) {
        return new FlagAndValue(s, null);
      }

      return new FlagAndValue(s.substring(0, idx), s.substring(idx + 1, s.length()));
    }
  }

  /**
   * Builds a {@code CLIOptions} object from CLI flags and arguments.
   *
   * @param args the arguments to the CLI tool.
   */
  public static CLIOptions parse(Iterable<String> args) throws IllegalArgumentException {
    CLIOptions.Builder optsBuilder = CLIOptions.builder();
    Iterator<String> it = args.iterator();
    while (it.hasNext()) {
      String option = it.next();
      if (!option.startsWith("-")) {
        optsBuilder.file(option);
        break;
      }

      FlagAndValue fv = FlagAndValue.fromString(option);
      switch (fv.flag) {
        case "--verbose":
        case "-verbose":
        case "-v":
          optsBuilder.verbose(true);
          break;
        case "--fix-only":
          optsBuilder.fixOnly(true);
          break;
        case "--replace":
        case "-replace":
        case "-r":
        case "-w":
          optsBuilder.replace(true);
          break;
        case "--help":
        case "-help":
        case "-h":
          optsBuilder.help(true);
          break;
        case "--version":
        case "-version":
          optsBuilder.version(true);
          break;
        default:
          throw new IllegalArgumentException("unexpected flag: " + fv.flag);
      }
    }

    return optsBuilder.build();
  }
}
