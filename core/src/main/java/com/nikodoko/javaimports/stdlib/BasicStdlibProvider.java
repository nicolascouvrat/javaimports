package com.nikodoko.javaimports.stdlib;

import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.stdlib.internal.Stdlib;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicStdlibProvider implements StdlibProvider {
  private Stdlib stdlib;

  BasicStdlibProvider(Stdlib stdlib) {
    this.stdlib = stdlib;
  }

  @Override
  public Map<String, Import> find(Iterable<String> identifiers) {
    Map<String, Import> candidates = findExactlyOneMatch(identifiers);
    for (String identifier : identifiers) {
      if (hasMultipleMatches(identifier)) {
        candidates.put(identifier, findBestMatch(identifier));
      }
    }

    return candidates;
  }

  private Map<String, Import> findExactlyOneMatch(Iterable<String> identifiers) {
    Map<String, Import> candidates = new HashMap<>();
    for (String identifier : identifiers) {
      Import[] found = stdlib.getClasses().get(identifier);
      if (found == null || found.length != 1) {
        continue;
      }

      candidates.put(identifier, found[0]);
    }

    return candidates;
  }

  private boolean hasMultipleMatches(String identifier) {
    Import[] matches = stdlib.getClasses().get(identifier);
    return matches != null && matches.length > 1;
  }

  private Import findBestMatch(String identifier) {
    Import[] matches = stdlib.getClasses().get(identifier);

    List<Import> filtered = selectShortestPaths(matches);
    if (filtered.size() == 1) {
      return filtered.get(0);
    }

    return selectJavaUtilOrFirstOne(filtered);
  }

  // heuristic that is debatable, but a look at conflicts for the Java8 stdlib tends to show that
  // more common imports have a shorter import path.
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

  // arbitrary rule, that exists mostly for cases like List, where the two candidates are java.util
  // and java.awt (and we almost always want the first one).
  private Import selectJavaUtilOrFirstOne(List<Import> imports) {
    for (Import i : imports) {
      if (i.isInJavaUtil()) {
        return i;
      }
    }

    return imports.get(0);
  }
}
