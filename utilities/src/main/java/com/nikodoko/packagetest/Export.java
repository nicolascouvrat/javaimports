package com.nikodoko.packagetest;

import com.google.common.io.Files;
import com.nikodoko.packagetest.exporters.Kind;
import java.io.File;
import java.util.List;

public class Export {
  private Export() {}

  public static Exported of(Kind exporter, List<Module> modules) {
    File temp = Files.createTempDir();
    return null;
  }
}
