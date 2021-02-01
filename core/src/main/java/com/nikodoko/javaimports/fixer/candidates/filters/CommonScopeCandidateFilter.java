package com.nikodoko.javaimports.fixer.candidates.filters;

import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.fixer.candidates.Candidate;
import com.nikodoko.javaimports.fixer.candidates.Candidates;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class CommonScopeCandidateFilter implements CandidateFilter {
  private static class ScopeCounter {
    private Map<Selector, Integer> scopeCounter = new HashMap<>();

    void add(List<Candidate> candidates) {
      candidates.stream()
          .map(c -> scope(c))
          .forEach(scope -> scopeCounter.put(scope, get(scope) + 1));
    }

    int get(Selector scope) {
      return scopeCounter.getOrDefault(scope, 0);
    }
  }

  // Candidates that are the only choice for their selector
  private List<Candidate> selected = new ArrayList<>();
  private ScopeCounter scopeCounter = new ScopeCounter();

  @Override
  public Candidates filter(Candidates candidates) {
    extractNonAmbiguous(candidates);
    buildScopeCounter();

    return candidates.selectors().stream()
        .map(s -> mostRelevantforSelector(s, candidates.getFor(s)))
        .reduce(Candidates::merge)
        .orElse(Candidates.EMPTY);
  }

  private void extractNonAmbiguous(Candidates candidates) {
    var nonAmbiguous =
        candidates.selectors().stream()
            .filter(s -> candidates.getFor(s).size() == 1)
            .flatMap(s -> candidates.getFor(s).stream())
            .collect(Collectors.toList());
    selected.addAll(nonAmbiguous);
  }

  private void buildScopeCounter() {
    scopeCounter.add(selected);
  }

  private Candidates mostRelevantforSelector(Selector selector, List<Candidate> candidates) {
    var topCount = highestCount(candidates);
    var best =
        candidates.stream()
            .filter(c -> scopeCounter.get(scope(c)) == topCount)
            .collect(Collectors.toList());
    return Candidates.forSelector(selector).add(best).build();
  }

  private int highestCount(List<Candidate> candidates) {
    var top =
        Collections.max(
            candidates, (c1, c2) -> scopeCounter.get(scope(c1)) - scopeCounter.get(scope(c2)));
    return scopeCounter.get(scope(top));
  }

  private static Selector scope(Candidate candidate) {
    return candidate.i.selector.scope();
  }
}
