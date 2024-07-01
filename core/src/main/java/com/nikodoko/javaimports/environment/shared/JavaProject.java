package com.nikodoko.javaimports.environment.shared;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.common.JavaSourceFile;
import com.nikodoko.javaimports.common.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Encapsulates all files in a Java project. */
public class JavaProject {
  private Map<Selector, List<JavaSourceFile>> filesByPackage = new HashMap<>();

  public void add(JavaSourceFile file) {
    var filesInPackage = filesByPackage.getOrDefault(file.pkg(), new ArrayList<>());
    filesInPackage.add(file);
    filesByPackage.put(file.pkg(), filesInPackage);
  }

  public List<JavaSourceFile> filesInPackage(Selector pkg) {
    return filesByPackage.getOrDefault(pkg, List.of());
  }

  public List<JavaSourceFile> allFiles() {
    return filesByPackage.values().stream().flatMap(List::stream).toList();
  }

  public String toString() {
    return MoreObjects.toStringHelper(this).add("filesByPackage", filesByPackage).toString();
  }
}
