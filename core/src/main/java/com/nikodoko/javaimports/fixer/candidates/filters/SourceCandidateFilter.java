package com.nikodoko.javaimports.fixer.candidates.filters;

import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.fixer.candidates.Candidate;
import com.nikodoko.javaimports.fixer.candidates.Candidates;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class SourceCandidateFilter implements CandidateFilter {
  public SourceCandidateFilter() {}

  @Override
  public Candidates filter(Candidates candidates) {
    return candidates.selectors().stream()
        .map(s -> mostRelevantForSelector(s, candidates.getFor(s)))
        .reduce(Candidates::merge)
        .orElse(Candidates.EMPTY);
  }

  private Candidates mostRelevantForSelector(Selector selector, List<Candidate> candidates) {
    var source = mostRelevantSource(candidates);
    var relevant = candidates.stream().filter(c -> c.s == source).collect(Collectors.toList());
    return Candidates.forSelector(selector).add(relevant).build();
  }

  private Candidate.Source mostRelevantSource(List<Candidate> candidates) {
    return Collections.max(candidates, (c1, c2) -> sourceValue(c1) - sourceValue(c2)).s;
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
