package com.nikodoko.javaimports.fixer.candidates.filters;

import com.nikodoko.javaimports.fixer.candidates.Candidate;

public class CandidateFilters {
  private static CandidateFilter compose(CandidateFilter... filters) {
    return candidates -> {
      var filtered = candidates;
      for (var filter : filters) {
        filtered = filter.filter(filtered);
      }

      return filtered;
    };
  }

  public static CandidateFilter of(Candidate.Source source) {
    switch (source) {
      case STDLIB:
        return compose(new SourceCandidateFilter(source), new StdlibCandidateFilter());
      default:
        return new SourceCandidateFilter(source);
    }
  }
}
