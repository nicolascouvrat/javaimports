package com.nikodoko.importer;

import java.util.Set;

/** An object representing a Java source file. */
public class ParsedFile {
  String packageName;
  Set<Import> imports;
  Scope scope;

  /**
   * A {@code ParsedFile} constructor.
   *
   * @param packageName its package name
   * @param imports its list of imports
   * @param scope its scope (the package scope, but limited to this file)
   */
  public ParsedFile(String packageName, Set<Import> imports, Scope scope) {
    this.packageName = packageName;
    this.imports = imports;
    this.scope = scope;
  }
}
