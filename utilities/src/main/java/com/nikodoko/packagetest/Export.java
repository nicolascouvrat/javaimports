package com.nikodoko.packagetest;

import com.google.common.io.Files;
import com.nikodoko.packagetest.exporters.Exporter;
import com.nikodoko.packagetest.exporters.ExporterFactory;
import com.nikodoko.packagetest.exporters.Kind;
import java.io.File;
import java.util.List;
import java.util.Map;

public class Export {
  private Export() {}

  public static Exported of(Kind exporterKind, List<Module> modules) {
    File temp = Files.createTempDir();
    Exported exported = new Exported(temp.toPath());
    Exporter exporter = ExporterFactory.create(exporterKind);
    for (Module m : modules) {
      exportModule(exported, exporter, m);
    }

    return exported;
  }

  private static void exportModule(Exported exported, Exporter exporter, Module module) {
    for (Map.Entry<String, String> file : module.files().entrySet()) {
      System.out.println(exporter.filename(exported.root(), module.name(), file.getKey()));
    }
  }
}
