package com.nikodoko.javaimports.parser;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Range;
import com.nikodoko.javaimports.common.ClassProvider;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.ImportProvider;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.parser.internal.ClassEntity;
import com.nikodoko.javaimports.parser.internal.Scope;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCImport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** An object representing a Java source file. */
public class ParsedFile implements ImportProvider, ClassProvider {
  // The name of the package to which this file belongs
  String packageName;
  // The imports in this file
  Map<String, Import> imports;
  // The package scope, limited to this file only
  Scope topScope = new Scope();
  ClassHierarchy classHierarchy = ClassHierarchies.root();
  // The position of the end of the package clause
  int packageEndPos;
  List<Range<Integer>> duplicates;
  Map<com.nikodoko.javaimports.common.Import, com.nikodoko.javaimports.common.ClassEntity> classes;

  /**
   * A {@code ParsedFile} constructor.
   *
   * @param packageName its package name
   * @param imports its imports, in a identifier:import map (for {@code import java.util.List;}, the
   *     identifier is {@code List})
   * @param packageEndPos the position of the end of its package clause
   * @param scope its scope (the package scope, but limited to this file)
   */
  private ParsedFile(
      String packageName,
      int packageEndPos,
      List<Range<Integer>> duplicates,
      Map<String, Import> imports) {
    this.packageName = packageName;
    this.packageEndPos = packageEndPos;
    this.duplicates = duplicates;
    this.imports = imports;
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

    return new ParsedFile(packageName, packageEndPos, duplicates, imports);
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

  public Selector pkg() {
    return Selector.of(Arrays.asList(packageName.split("\\.")));
  }

  /** An identifier:import map of imports in this {@code ParsedFile} */
  public Map<String, Import> imports() {
    return imports;
  }

  /**
   * Attach the given {@code scope} to this {@code ParsedFile}.
   *
   * @param topScope the scope to attach
   */
  public ParsedFile topScope(Scope topScope) {
    this.topScope = topScope;
    return this;
  }

  public ParsedFile classHierarchy(ClassHierarchy classHierarchy) {
    this.classHierarchy = classHierarchy;
    this.classes =
        ClassHierarchies.entities(classHierarchy).stream()
            .collect(
                Collectors.toMap(
                    // Classes are never imported as static
                    c -> new com.nikodoko.javaimports.common.Import(pkg().combine(c.name), false),
                    Function.identity(),
                    // We should not get 2 classes with the exact same import coming from one file.
                    // Should probably spit out a log if we do
                    (a, b) -> a));
    return this;
  }

  @Override
  public Optional<com.nikodoko.javaimports.common.ClassEntity> findClass(
      com.nikodoko.javaimports.common.Import i) {
    // System.out.println(String.format("Looking for %s in %s", i, byImport));
    return Optional.ofNullable(classes.get(i));
  }

  public Set<String> notYetResolved() {
    return topScope.notYetResolved;
  }

  public Set<ClassExtender> notFullyExtendedClasses() {
    return topScope.notFullyExtended;
  }

  public Set<String> topLevelDeclarations() {
    return topScope.identifiers;
  }

  public ClassHierarchy classHierarchy() {
    return classHierarchy;
  }

  public Stream<ClassEntity> classes() {
    return ClassHierarchies.flatView(classHierarchy);
  }

  // TODO: maybe have a SiblingFile with the below findImports, and make this the default
  // findImports of ParsedFile
  public Collection<com.nikodoko.javaimports.common.Import> findImportables(Identifier identifier) {
    var importables =
        classes.keySet().stream().collect(Collectors.groupingBy(i -> i.selector.identifier()));
    return Optional.ofNullable(importables.get(identifier)).orElse(List.of());
  }

  // TODO: remove
  public Collection<com.nikodoko.javaimports.common.Import> findImports(Identifier i) {
    if (!imports.containsKey(i.toString())) {
      return List.of();
    }

    return List.of(imports.get(i.toString()).toNew());
  }

  /** Debugging support. */
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("packageName", packageName)
        .add("imports", imports)
        .add("topScope", topScope)
        .add("packageEndPos", packageEndPos)
        .add("duplicates", duplicates)
        .add("classHierarchy", classHierarchy)
        .toString();
  }
}
