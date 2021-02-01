package com.nikodoko.javaimports.fixer.candidates.filters;

import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.fixer.candidates.Candidate;
import com.nikodoko.javaimports.fixer.candidates.Candidates;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class StdlibCandidateFilter implements CandidateFilter {
  private static final Selector JAVA_UTIL = Selector.of("java", "util");

  @Override
  public Candidates filter(Candidates candidates) {
    return candidates.selectors().stream()
        .map(s -> mostRelevantForSelector(s, candidates.getFor(s)))
        .reduce(Candidates::merge)
        .orElse(Candidates.EMPTY);
  }

  private Candidates mostRelevantForSelector(Selector selector, List<Candidate> candidates) {
    if (candidates.stream().filter(c -> c.s != Candidate.Source.STDLIB).count() != 0) {
      throw new IllegalArgumentException("Some candidates do not come from the stdlib");
    }

    var shortest = selectShortest(candidates);
    var relevant = selectJavaUtilIfAny(shortest);

    return Candidates.forSelector(selector).add(relevant).build();
  }

  private List<Candidate> selectShortest(List<Candidate> candidates) {
    var byLength = candidates.stream().collect(Collectors.groupingBy(c -> c.i.selector.size()));
    var minLength = Collections.min(byLength.keySet());
    return byLength.get(minLength);
  }

  private List<Candidate> selectJavaUtilIfAny(List<Candidate> candidates) {
    var inJavaUtil =
        candidates.stream()
            .filter(c -> c.i.selector.startsWith(JAVA_UTIL))
            .collect(Collectors.toList());
    if (inJavaUtil.isEmpty()) {
      return candidates;
    }

    return inJavaUtil;
  }
}
