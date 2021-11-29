package com.nikodoko.javaimports.fixer.candidates;

import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.fixer.candidates.filters.CandidateFilters;

public class BasicCandidateSelectionStrategy implements CandidateSelectionStrategy {
  private final Selector pkg;

  public BasicCandidateSelectionStrategy(Selector pkg) {
    this.pkg = pkg;
  }

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
    return CandidateFilters.sourceSpecificRules(pkg).filter(candidates);
  }

  private BestCandidates takeFirst(Candidates candidates) {
    return new TakeFirstCandidateSelectionStrategy().selectBest(candidates);
  }
}
