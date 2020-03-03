package com.nikodoko.javaimports.parser;

import com.google.common.base.MoreObjects;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.Set;
import java.util.stream.Collectors;

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

  /**
   * Creates a {@code ParsedFile} from a {@link JCCompilationUnit}.
   *
   * <p>The resulting {@code ParsedFile} has a scope of null.
   *
   * @param unit the compilation unit to use
   */
  public static ParsedFile fromCompilationUnit(JCCompilationUnit unit) {
    Set<Import> imports =
        unit.getImports().stream().map(Import::fromJcImport).collect(Collectors.toSet());
    String packageName = unit.getPackageName().toString();
    return new ParsedFile(packageName, imports, null);
  }

  /**
   * Attach the given {@code scope} to this {@code ParsedFile}.
   *
   * @param scope the scope to attach
   */
  public void attachScope(Scope scope) {
    this.scope = scope;
  }

  /** Debugging support. */
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("packageName", packageName)
        .add("imports", imports)
        .add("scope", scope)
        .toString();
  }
}
