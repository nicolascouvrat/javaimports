package com.nikodoko.javaimports.fixer.candidates;

/** A strategy that simply returns the first available candidate for each selector. */
public class TakeFirstCandidateSelectionStrategy implements CandidateSelectionStrategy {
  @Override
  public BestCandidates selectBest(Candidates candidates) {
    var best = BestCandidates.builder();
    for (var selector : candidates.selectors()) {
      best.put(selector, candidates.getFor(selector).get(0).i);
    }

    return best.build();
  }
}
