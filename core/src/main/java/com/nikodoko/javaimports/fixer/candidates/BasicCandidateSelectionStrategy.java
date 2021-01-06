package com.nikodoko.javaimports.fixer.candidates;

import com.nikodoko.javaimports.common.Import;
import java.util.Collection;
import java.util.Collections;

public class BasicCandidateSelectionStrategy implements CandidateSelectionStrategy {
  @Override
  public BestCandidates selectBest(Candidates candidates) {
    var builder = BestCandidates.builder();
    for (var selector : candidates.selectors()) {
      builder.put(selector, bestSource(candidates.getFor(selector)));
    }

    return builder.build();
  }

  private Import bestSource(Collection<Candidate> candidates) {
    return Collections.max(candidates, (c1, c2) -> sourceValue(c1) - sourceValue(c2)).i;
  }

  private int sourceValue(Candidate candidate) {
    switch (candidate.s) {
      case SIBLING:
        return 2;
      case STDLIB:
        return 1;
      case EXTERNAL:
        return 0;
      default:
        return -1;
    }
  }
}
