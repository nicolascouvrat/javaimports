package com.nikodoko.javaimports.fixer.candidates.filters;

import com.nikodoko.javaimports.fixer.candidates.Candidate;
import java.util.List;
import java.util.stream.Collectors;

public class SourceCandidateFilter implements CandidateFilter {
  private final Candidate.Source source;

  public SourceCandidateFilter(Candidate.Source source) {
    this.source = source;
  }

  @Override
  public List<Candidate> filter(List<Candidate> candidates) {
    return candidates.stream().filter(c -> c.s == source).collect(Collectors.toList());
  }
}
