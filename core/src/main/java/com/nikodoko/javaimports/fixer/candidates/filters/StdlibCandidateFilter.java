package com.nikodoko.javaimports.fixer.candidates.filters;

import com.nikodoko.javaimports.fixer.candidates.Candidate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class StdlibCandidateFilter implements CandidateFilter {
  @Override
  public List<Candidate> filter(List<Candidate> candidates) {
    var byLength = candidates.stream().collect(Collectors.groupingBy(c -> c.i.selector.size()));
    var minLength = Collections.min(byLength.keySet());
    return byLength.get(minLength);
  }
}
