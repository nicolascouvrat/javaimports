package com.nikodoko.javaimports.fixer.candidates.filters;

import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.fixer.candidates.Candidate;
import com.nikodoko.javaimports.fixer.candidates.Candidates;
import java.util.List;

public class CandidateFilters {
  private static CandidateFilter NOOP = candidates -> candidates;

  public static CandidateFilter mostRelevantSource() {
    return new SourceCandidateFilter();
  }

  public static CandidateFilter mostCommonScope() {
    return new CommonScopeCandidateFilter();
  }

  public static CandidateFilter sourceSpecificRules() {
    return candidates ->
        candidates.selectors().stream()
            .map(s -> filterCandidatesOfSameSource(s, candidates.getFor(s)))
            .reduce(Candidates::merge)
            .orElse(Candidates.EMPTY);
  }

  private static Candidates filterCandidatesOfSameSource(Selector s, List<Candidate> candidates) {
    if (candidates.isEmpty()) {
      throw new IllegalArgumentException("Why do we have a selector with no candidates?");
    }

    var source = candidates.get(0).s;
    if (candidates.stream().filter(c -> c.s != source).count() != 0) {
      throw new IllegalArgumentException(
          String.format("Candidates for selector %s have different sources"));
    }

    return rulesForSource(source).filter(Candidates.forSelector(s).add(candidates).build());
  }

  private static CandidateFilter rulesForSource(Candidate.Source source) {
    switch (source) {
      case STDLIB:
        return new StdlibCandidateFilter();
      default:
        return NOOP;
    }
  }
}
