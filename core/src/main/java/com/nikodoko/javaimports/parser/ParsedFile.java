package com.nikodoko.javaimports.parser;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.openjdk.tools.javac.tree.JCTree.JCCompilationUnit;
import org.openjdk.tools.javac.tree.JCTree.JCExpression;
import org.openjdk.tools.javac.tree.JCTree.JCImport;

/** An object representing a Java source file. */
public class ParsedFile {
  // The name of the package to which this file belongs
  String packageName;
  // The imports in this file
  Map<String, Import> imports;
  // The package scope, limited to this file only
  Scope scope;
  // The position of the end of the package clause
  int packageEndPos;
  List<Range<Integer>> duplicates;

  /**
   * A {@code ParsedFile} constructor.
   *
   * @param packageName its package name
   * @param imports its imports, in a identifier:import map (for {@code import java.util.List;}, the
   *     identifier is {@code List})
   * @param packageEndPos the position of the end of its package clause
   * @param scope its scope (the package scope, but limited to this file)
   */
  public ParsedFile(
      String packageName,
      int packageEndPos,
      List<Range<Integer>> duplicates,
      Map<String, Import> imports,
      Scope scope) {
    this.packageName = packageName;
    this.packageEndPos = packageEndPos;
    this.duplicates = duplicates;
    this.imports = imports;
    this.scope = scope;
  }

  private static int findEndOfPackageClause(JCCompilationUnit unit) {
    JCExpression pkg = (JCExpression) unit.getPackageName();
    return pkg.getEndPosition(unit.endPositions);
  }

  private static Range<Integer> rangeOf(JCCompilationUnit unit, JCImport statement) {
    return Range.closed(statement.getStartPosition(), statement.getEndPosition(unit.endPositions));
  }

  /**
   * Creates a {@code ParsedFile} from a {@link JCCompilationUnit}.
   *
   * <p>The resulting {@code ParsedFile} has a scope of null.
   *
   * @param unit the compilation unit to use
   */
  public static ParsedFile fromCompilationUnit(JCCompilationUnit unit) {
    String packageName = unit.getPackageName().toString();

    int packageEndPos = findEndOfPackageClause(unit);
    Map<String, Import> imports = new HashMap<>();
    List<Range<Integer>> duplicates = new ArrayList<>();
    // unit.getImports() potentially contains the same import multiple times, in which case we want
    // to
    // consider only one of them (and mark the others as duplicates)
    for (JCImport existingImport : unit.getImports()) {
      Import i = Import.fromJcImport(existingImport);
      if (!imports.containsKey(i.name())) {
        imports.put(i.name(), i);
        continue;
      }

      duplicates.add(rangeOf(unit, existingImport));
    }

    return new ParsedFile(packageName, packageEndPos, duplicates, imports, null);
  }

  /** The position of the end of this {@code ParsedFile}'s package clause */
  public int packageEndPos() {
    return packageEndPos;
  }

  /** The [startPosition, endPosition] of duplicate imports, if any */
  public List<Range<Integer>> duplicates() {
    return duplicates;
  }

  /** The package to which this {@code ParsedFile} belongs */
  public String packageName() {
    return packageName;
  }

  /** An identifier:import map of imports in this {@code ParsedFile} */
  public Map<String, Import> imports() {
    return imports;
  }

  /** The package {@link Scope}, limited to this current {@code ParsedFile} */
  @Nullable
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
        .add("packageEndPos", packageEndPos)
        .add("duplicates", duplicates)
        .toString();
  }
}