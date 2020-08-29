package com.nikodoko.javaimports.environment;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.nikodoko.javaimports.parser.ParsedFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Encapsulates all files in a Java project. */
public class JavaProject {
  private Map<String, Set<ParsedFile>> filesByPackage = new HashMap<>();

  public void add(ParsedFile file) {
    Set<ParsedFile> filesInPackage =
        filesByPackage.getOrDefault(file.packageName(), new HashSet<>());
    filesInPackage.add(file);
    filesByPackage.put(file.packageName(), filesInPackage);
  }

  public Iterable<ParsedFile> filesInPackage(String pkg) {
    return filesByPackage.getOrDefault(pkg, new HashSet<>());
  }

  public Iterable<ParsedFile> allFiles() {
    return Iterables.concat(filesByPackage.values());
  }

  public String toString() {
    return MoreObjects.toStringHelper(this).add("filesByPackage", filesByPackage).toString();
  }
}
