package com.nikodoko.javaimports.parser;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCImport;
import java.util.Map;

/** An object representing a Java source file. */
public class ParsedFile {
  String packageName;
  Map<String, Import> imports;
  Scope scope;
  int packageEndPos;

  /**
   * A {@code ParsedFile} constructor.
   *
   * @param packageName its package name
   * @param imports a map of identifier:import
   * @param scope its scope (the package scope, but limited to this file)
   */
  public ParsedFile(
      String packageName, int packageEndPos, Map<String, Import> imports, Scope scope) {
    this.packageName = packageName;
    this.packageEndPos = packageEndPos;
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
    ImmutableMap.Builder<String, Import> builder = ImmutableMap.builder();
    for (JCImport existingImport : unit.getImports()) {
      Import i = Import.fromJcImport(existingImport);
      builder.put(i.name(), i);
    }

    JCExpression pkg = (JCExpression) unit.getPackageName();
    String packageName = unit.getPackageName().toString();
    int packageEndPos = pkg.getEndPosition(unit.endPositions);
    return new ParsedFile(packageName, packageEndPos, builder.build(), null);
  }

  public int packageEndPos() {
    return packageEndPos;
  }

  public String packageName() {
    return packageName;
  }

  public Map<String, Import> imports() {
    return imports;
  }

  public Scope scope() {
    return scope;
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
