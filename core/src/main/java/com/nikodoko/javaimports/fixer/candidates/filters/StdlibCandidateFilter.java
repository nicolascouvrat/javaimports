package com.nikodoko.javaimports.fixer.candidates.filters;

import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.fixer.candidates.Candidate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class StdlibCandidateFilter implements CandidateFilter {
  private static final Selector JAVA_UTIL = Selector.of("java", "util");

  @Override
  public List<Candidate> filter(List<Candidate> candidates) {
    var shortest = selectShortest(candidates);
    var javaUtil = selectJavaUtil(shortest);
    if (!javaUtil.isEmpty()) {
      return javaUtil;
    }

    return shortest;
  }

  public List<Candidate> selectShortest(List<Candidate> candidates) {
    var byLength = candidates.stream().collect(Collectors.groupingBy(c -> c.i.selector.size()));
    var minLength = Collections.min(byLength.keySet());
    return byLength.get(minLength);
  }

  public List<Candidate> selectJavaUtil(List<Candidate> candidates) {
    return candidates.stream()
        .filter(c -> c.i.selector.startsWith(JAVA_UTIL))
        .collect(Collectors.toList());
  }
}
