package com.nikodoko.javaimports.fixer.candidates.filters;

import com.nikodoko.javaimports.fixer.candidates.Candidate;

public class CandidateFilters {
  public static CandidateFilter of(Candidate.Source source) {
    switch (source) {
      default:
        return new SourceCandidateFilter(source);
    }
  }
}
