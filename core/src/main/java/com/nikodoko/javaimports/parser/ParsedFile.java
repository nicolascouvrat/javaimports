package com.nikodoko.javaimports.parser;

import com.google.common.collect.Range;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.ClassProvider;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.ImportProvider;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.parser.internal.Scope;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
    Map<Import, ClassEntity> classMap)
    implements ImportProvider, ClassProvider {
  /** The package to which this {@code ParsedFile} belongs */
  public String packageName() {
    return pkg().toString();
  }

  /** A list of identifiers provided by the imports at the top of this file */
  public Set<Identifier> importedIdentifiers() {
    return imports.keySet();
  }

  @Override
  public Optional<ClassEntity> findClass(Import i) {
    return Optional.ofNullable(classMap.get(i));
  }

  public Orphans orphans() {
    return Orphans.wrapping(topScope());
  }

  public Set<Identifier> topLevelDeclarations() {
    return topScope().identifiers;
  }

  // Used only for tests
  public Stream<ClassEntity> classes() {
    return classMap.values().stream();
  }

  // TODO: maybe have a SiblingFile with the below findImports, and make this the default
  // findImports of ParsedFile
  public Collection<Import> findImportables(Identifier identifier) {
    var importables =
        classMap().keySet().stream().collect(Collectors.groupingBy(i -> i.selector.identifier()));
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
      return new ParsedFile(
          pkg, packageEndPos, duplicateImportPositions, imports, topScope, classes);
    }
  }
}
