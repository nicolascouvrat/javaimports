package com.nikodoko.javaimports.stdlib;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.environment.jarutil.JarIdentifierLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FakeStdlibProvider implements StdlibProvider {
  Map<String, Import> imports = new HashMap<>();

  public static FakeStdlibProvider of(com.nikodoko.javaimports.parser.Import... imports) {
    Map<String, Import> byIdentifier = new HashMap<>();
    for (var i : imports) {
      byIdentifier.put(i.name(), i.toNew());
    }

    return new FakeStdlibProvider(byIdentifier);
  }

  private FakeStdlibProvider(Map<String, Import> imports) {
    this.imports = imports;
  }

  @Override
  public Optional<ClassEntity> findClass(Import i) {
    if (!imports.values().contains(i)) {
      return Optional.empty();
    }

    var loader = new JarIdentifierLoader(List.of());
    var c = ClassEntity.named(i.selector).declaring(loader.loadIdentifiers(i)).build();
    return Optional.of(c);
  }

  @Override
  public Collection<Import> findImports(Identifier i) {
    var found = imports.get(i.toString());
    if (found == null) {
      return List.of();
    }

    return List.of(found);
  }

  @Override
  public boolean isInJavaLang(String identifier) {
    return false;
  }
}
