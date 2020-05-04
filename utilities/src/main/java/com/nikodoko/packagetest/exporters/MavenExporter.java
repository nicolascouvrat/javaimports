package com.nikodoko.packagetest.exporters;

class MavenExporter implements Exporter {
  public static final String NAME = "MAVEN_EXPORTER";

  @Override
  public String name() {
    return NAME;
  }
}
