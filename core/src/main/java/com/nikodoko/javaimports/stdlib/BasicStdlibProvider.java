package com.nikodoko.javaimports.stdlib;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.stdlib.internal.Stdlib;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BasicStdlibProvider implements StdlibProvider {
  private Stdlib stdlib;

  BasicStdlibProvider(Stdlib stdlib) {
    this.stdlib = stdlib;
  }

  @Override
  public Map<String, Import> find(Iterable<String> identifiers) {
    Map<String, Import> found = new HashMap<>();
    Set<String> leftovers = new HashSet<>();
    for (String identifier : identifiers) {
      if (!stdlib.getClasses().containsKey(identifier)) {
        continue;
      }

      if (stdlib.getClasses().get(identifier).length == 1) {
        found.put(identifier, stdlib.getClasses().get(identifier)[0]);
        continue;
      }

      leftovers.add(identifier);
    }

    for (String leftover : leftovers) {
      found.put(leftover, selectImport(stdlib.getClasses().get(leftover)));
    }

    return found;
  }

  private Import selectImport(Import[] imports) {
    checkNotNull(imports, "why are imports null?");
    checkArgument(imports.length > 0, "why are imports empty?");
    if (imports.length == 1) {
      return imports[0];
    }

    List<Import> filtered = selectShortestPaths(imports);
    if (filtered.size() == 1) {
      return filtered.get(0);
    }

    return selectJavaUtilOrFirstOne(filtered);
  }

  private List<Import> selectShortestPaths(Import[] imports) {
    List<Import> candidates = new ArrayList<>();
    int currentShortestPath = imports[0].pathLength();
    for (Import i : imports) {
      if (i.pathLength() > currentShortestPath) {
        continue;
      }

      if (i.pathLength() == currentShortestPath) {
        candidates.add(i);
        continue;
      }

      candidates = new ArrayList<Import>();
      candidates.add(i);
      currentShortestPath = i.pathLength();
    }

    return candidates;
  }

  private Import selectJavaUtilOrFirstOne(List<Import> imports) {
    for (Import i : imports) {
      if (i.isInJavaUtil()) {
        return i;
      }
    }

    return imports.get(0);
  }
}
