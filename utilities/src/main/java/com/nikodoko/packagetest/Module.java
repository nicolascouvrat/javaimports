package com.nikodoko.packagetest;

import java.util.Map;

public class Module {
  private String name;
  private Map<String, String> files;

  public Module(String name, Map<String, String> files) {
    this.name = name;
    this.files = files;
  }

  public String name() {
    return name;
  }

  public Map<String, String> files() {
    return files;
  }
}
