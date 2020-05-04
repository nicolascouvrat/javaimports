package com.nikodoko.packagetest;

import java.nio.file.Path;

public class Exported {
  private Path root;

  public Path root() {
    return root;
  }

  public Path file(String module, String fragment) {
    return null;
  }
}
