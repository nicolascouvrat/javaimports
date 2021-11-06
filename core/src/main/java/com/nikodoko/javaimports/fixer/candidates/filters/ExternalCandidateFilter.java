package com.nikodoko.javaimports.fixer.candidates.filters;

import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.fixer.candidates.Candidate;
import com.nikodoko.javaimports.fixer.candidates.Candidates;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class ExternalCandidateFilter implements CandidateFilter {
  private final Selector.Distance distance;

  ExternalCandidateFilter(Selector pkg) {
    this.distance = Selector.Distance.from(pkg);
  }

  @Override
  public Candidates filter(Candidates candidates) {
    return candidates.selectors().stream()
        .map(s -> closestToCurrentPackage(s, candidates.getFor(s)))
        .reduce(Candidates::merge)
        .orElse(Candidates.EMPTY);
  }

  private Candidates closestToCurrentPackage(Selector selector, List<Candidate> candidates) {
    if (candidates.stream().filter(c -> c.s != Candidate.Source.EXTERNAL).count() != 0) {
      throw new IllegalArgumentException("Some candidates do not come from external dependencies");
    }

    if (candidates.isEmpty()) {
      return Candidates.EMPTY;
    }

    var byDistance =
        candidates.stream().collect(Collectors.groupingBy(c -> distance.to(c.i.selector)));
    var minDistance = Collections.min(byDistance.keySet());
    return Candidates.forSelector(selector).add(byDistance.get(minDistance)).build();
  }
}
