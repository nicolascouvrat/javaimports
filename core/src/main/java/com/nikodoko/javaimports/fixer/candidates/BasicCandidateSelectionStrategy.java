package com.nikodoko.javaimports.fixer.candidates;

import com.nikodoko.javaimports.fixer.candidates.filters.CandidateFilters;

public class BasicCandidateSelectionStrategy implements CandidateSelectionStrategy {
  @Override
  public BestCandidates selectBest(Candidates candidates) {
    var filtered = bestSource(candidates);
    filtered = mostCommonScope(filtered);
    filtered = sourceSpecificRules(filtered);
    return takeFirst(filtered);
  }

  private Candidates bestSource(Candidates candidates) {
    return CandidateFilters.mostRelevantSource().filter(candidates);
  }

  private Candidates mostCommonScope(Candidates candidates) {
    return CandidateFilters.mostCommonScope().filter(candidates);
  }

  private Candidates sourceSpecificRules(Candidates candidates) {
    return CandidateFilters.sourceSpecificRules().filter(candidates);
  }

  private BestCandidates takeFirst(Candidates candidates) {
    var best = BestCandidates.builder();
    for (var selector : candidates.selectors()) {
      best.put(selector, candidates.getFor(selector).get(0).i);
    }

    return best.build();
  }
}
