package com.nikodoko.javaimports.parser;

import com.google.common.collect.Range;
import com.nikodoko.javaimports.common.ClassDeclaration;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.ClassProvider;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.ImportProvider;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Superclass;
import com.nikodoko.javaimports.fixer.candidates.Candidate;
import com.nikodoko.javaimports.fixer.candidates.CandidateFinder;
import com.nikodoko.javaimports.parser.internal.Classes;
import com.nikodoko.javaimports.parser.internal.Scope;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** An object representing a Java source file. */
public record ParsedFile(
    Selector pkg,
    int packageEndPos,
    List<Range<Integer>> duplicates,
    Map<Identifier, Import> imports,
    Scope topScope,
    Classes classes)
    implements ImportProvider, ClassProvider {
  /** The package to which this {@code ParsedFile} belongs */
  public String packageName() {
    return pkg().toString();
  }

  /** A list of identifiers provided by the imports at the top of this file */
  public Set<Identifier> importedIdentifiers() {
    return imports.keySet();
  }

  /** Adds the provided declarations to the scope of this file. */
  public void addDeclarations(Set<Identifier> declarations) {
    declarations.forEach(topScope::declare);
  }

  /**
   * Snapshots the current state by returning all identifiers currently unresolved.
   *
   * <p>Be careful, as the underlying scope is mutable. Successive calls to unresolved() are not
   * guaranteed to return the same elements depending on other operations performed in between.
   */
  public Set<Identifier> unresolved() {
    Set<Identifier> collector = new HashSet<>();
    collectUnresolved(collector, topScope);
    return collector;
  }

  private static void collectUnresolved(Set<Identifier> collector, Scope scope) {
    collector.addAll(scope.unresolved);
    for (var s : scope.childScopes) {
      collectUnresolved(collector, s);
    }
  }

  @Override
  public Optional<ClassEntity> findClass(Import i) {
    return Optional.ofNullable(classes.reachable().get(i));
  }

  public Orphans orphans() {
    return Orphans.wrapping(topScope());
  }

  public Set<Identifier> topLevelDeclarations() {
    return topScope().identifiers;
  }

  // Used only for tests
  public Stream<ClassEntity> allClasses() {
    return Stream.concat(classes.reachable().values().stream(), classes.unreachable().stream());
  }

  // TODO: maybe have a SiblingFile with the below findImports, and make this the default
  // findImports of ParsedFile
  public Collection<Import> findImportables(Identifier identifier) {
    var importables =
        classes.reachable().keySet().stream()
            .collect(Collectors.groupingBy(i -> i.selector.identifier()));
    return Optional.ofNullable(importables.get(identifier)).orElse(List.of());
  }

  // TODO: remove
  @Override
  public Collection<Import> findImports(Identifier i) {
    if (!imports().containsKey(i)) {
      return List.of();
    }

    return List.of(imports().get(i));
  }

  public static Builder inPackage(Selector pkg, int pkgEndPos) {
    return new Builder(pkg, pkgEndPos);
  }

  public static class Builder {
    final Selector pkg;
    final int packageEndPos;
    Map<Identifier, Import> imports = new HashMap<>();
    Scope topScope = new Scope();
    List<Range<Integer>> duplicateImportPositions = new ArrayList<>();
    Map<Import, ClassEntity> classes = new HashMap<>();

    private Builder(Selector pkg, int packageEndPos) {
      this.pkg = pkg;
      this.packageEndPos = packageEndPos;
    }

    public Builder addImport(Import i) {
      this.imports.put(i.selector.identifier(), i);
      return this;
    }

    public Builder topScope(Scope s) {
      this.topScope = s;
      return this;
    }

    public Builder addDuplicateImportPosition(Range<Integer> pos) {
      this.duplicateImportPositions.add(pos);
      return this;
    }

    public Builder addDeclaredClass(Selector i, ClassEntity e) {
      this.classes.put(new Import(pkg.combine(i), false), e);
      return this;
    }

    public ParsedFile build() {
      traverseAndUpdate(topScope, imports);
      var classes = Classes.of(topScope, pkg);
      return new ParsedFile(
          pkg, packageEndPos, duplicateImportPositions, imports, topScope, classes);
    }

    // Do a first round of parent class resolution at the scope of the file, to mark superclasses
    // as resolved wherever possible
    private static void traverseAndUpdate(Scope topScope, Map<Identifier, Import> imports) {
      var finder = new CandidateFinder();
      finder.add(
          Candidate.Source.SIBLING,
          i -> {
            if (!imports.containsKey(i)) {
              return List.of();
            }

            return List.of(imports.get(i));
          });
      var queue = new ArrayDeque<Scope>();
      queue.add(topScope);

      while (!queue.isEmpty()) {
        var next = queue.pop();
        queue.addAll(next.childScopes);
        var maybeSuperclass = next.maybeClass.flatMap(ClassDeclaration::maybeParent);
        if (maybeSuperclass.isEmpty()) {
          continue;
        }
        var superClass = maybeSuperclass.get();
        if (!superClass.isResolved()) {
          var unresolved = superClass.getUnresolved();
          var candidates = finder.find(unresolved);
          var possible = candidates.getFor(unresolved);
          if (possible.isEmpty()) {
            continue;
          }

          if (possible.size() > 1) {
            throw new RuntimeException("unexpected candidates size");
          }

          var i = possible.get(0).i;
          var newDecl = new ClassDeclaration(next.maybeClass.get().name(), Superclass.resolved(i));
          next.maybeClass = Optional.of(newDecl);
        }
      }
    }
  }
}
