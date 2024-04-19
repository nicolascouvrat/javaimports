package com.nikodoko.javaimports.common.telemetry;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Logs {
  private static volatile Level level = Level.OFF;

  public static synchronized void enable() {
    level = Level.INFO;
  }

  public static Logger getLogger(String name) {
    var l = Logger.getLogger(name);
    l.setLevel(level);
    return l;
  }
}
