package com.nikodoko.javaimports.fixer.candidates;

public interface CandidateSelectionStrategy {
  /** Select the best candidate for each selector in {@code candidates}. */
  BestCandidates selectBest(Candidates candidates);
}
