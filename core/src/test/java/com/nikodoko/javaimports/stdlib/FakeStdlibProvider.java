package com.nikodoko.javaimports.stdlib;

import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.parser.Import;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakeStdlibProvider implements StdlibProvider {
  Map<String, Import> imports = new HashMap<>();

  public static FakeStdlibProvider of(Import... imports) {
    Map<String, Import> byIdentifier = new HashMap<>();
    for (Import i : imports) {
      byIdentifier.put(i.name(), i);
    }

    return new FakeStdlibProvider(byIdentifier);
  }

  private FakeStdlibProvider(Map<String, Import> imports) {
    this.imports = imports;
  }

  @Override
  public Map<String, Import> find(Iterable<String> identifiers) {
    Map<String, Import> found = new HashMap<>();
    for (String id : identifiers) {
      if (imports.containsKey(id)) {
        found.put(id, imports.get(id));
      }
    }

    return found;
  }

  @Override
  public Collection<com.nikodoko.javaimports.common.Import> findImports(Identifier i) {
    var found = imports.get(i.toString());
    if (found == null) {
      return List.of();
    }

    return List.of(found.toNew());
  }

  @Override
  public boolean isInJavaLang(String identifier) {
    return false;
  }
}
