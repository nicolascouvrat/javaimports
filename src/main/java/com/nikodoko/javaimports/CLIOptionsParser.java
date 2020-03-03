package com.nikodoko.javaimports;

import java.util.Iterator;

public class CLIOptionsParser {
  static CLIOptions parse(Iterable<String> args) {
    CLIOptions.Builder optsBuilder = CLIOptions.builder();
    Iterator<String> it = args.iterator();
    while (it.hasNext()) {
      String option = it.next();
      optsBuilder.file(option);
    }

    return optsBuilder.build();
  }
}
