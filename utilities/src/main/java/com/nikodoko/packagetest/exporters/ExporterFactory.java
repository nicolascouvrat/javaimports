package com.nikodoko.packagetest.exporters;

/** An {@link Exporter} factory. */
public class ExporterFactory {
  private ExporterFactory() {}

  /** Creates an {@link Exporter} of a given {@link Kind}. */
  public static Exporter create(Kind kind) {
    switch (kind) {
      case MAVEN:
        return new MavenExporter();
    }

    throw new IllegalArgumentException("unknown kind: " + kind);
  }
}
