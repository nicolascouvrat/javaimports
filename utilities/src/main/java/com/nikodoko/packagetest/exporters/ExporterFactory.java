package com.nikodoko.packagetest.exporters;

public class ExporterFactory {
  private ExporterFactory() {}

  public static Exporter create(Kind kind) {
    switch (kind) {
      case MAVEN:
        return new MavenExporter();
    }

    throw new IllegalArgumentException("unknown kind: " + kind);
  }
}
