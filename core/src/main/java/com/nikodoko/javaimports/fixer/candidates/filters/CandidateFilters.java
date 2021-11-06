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

  // Pkg is the package for which we are trying to find imports
  public static CandidateFilter sourceSpecificRules(Selector pkg) {
    return candidates ->
        candidates.selectors().stream()
            .map(s -> filterCandidatesOfSameSource(pkg, s, candidates.getFor(s)))
            .reduce(Candidates::merge)
            .orElse(Candidates.EMPTY);
  }

  private static Candidates filterCandidatesOfSameSource(
      Selector pkg, Selector s, List<Candidate> candidates) {
    if (candidates.isEmpty()) {
      throw new IllegalArgumentException("Why do we have a selector with no candidates?");
    }

    var source = candidates.get(0).s;
    if (candidates.stream().filter(c -> c.s != source).count() != 0) {
      throw new IllegalArgumentException(
          String.format("Candidates for selector %s have different sources"));
    }

    return rulesForSource(source, pkg).filter(Candidates.forSelector(s).add(candidates).build());
  }

  private static CandidateFilter rulesForSource(Candidate.Source source, Selector pkg) {
    switch (source) {
      case STDLIB:
        return new StdlibCandidateFilter();
      case EXTERNAL:
        return new ExternalCandidateFilter(pkg);
      default:
        return NOOP;
    }
  }
}
