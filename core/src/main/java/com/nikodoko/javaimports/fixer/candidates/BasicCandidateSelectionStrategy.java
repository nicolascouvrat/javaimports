package com.nikodoko.javaimports.fixer.candidates;

import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.fixer.candidates.filters.CandidateFilters;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicCandidateSelectionStrategy implements CandidateSelectionStrategy {
  @Override
  public BestCandidates selectBest(Candidates candidates) {
    var builder = BestCandidates.builder();
    for (var entry : bestImportBySelector(candidates).entrySet()) {
      builder.put(entry.getKey(), entry.getValue());
    }

    return builder.build();
  }

  private Map<Selector, Import> bestImportBySelector(Candidates candidates) {
    var mostRelevantBySelector = mostRelevantBySelector(candidates);
    var bestBySelector = new HashMap<Selector, Import>();
    for (var selector : mostRelevantBySelector.keySet()) {
      if (mostRelevantBySelector.get(selector).isEmpty()) {
        throw new IllegalStateException(
            String.format("Why do we have empty imports for selector=%s ??", selector));
      }

      bestBySelector.put(selector, mostRelevantBySelector.get(selector).get(0).i);
    }

    return bestBySelector;
    // var scopeCount = new Map<Selector, Integer>();
    // for (Import i : best.values()) {
    //   var currentCount = scopeCount.computeIfAbsent(i.selector.scope(), __ -> 0);
    //   scopeCount.put(i.selector.scope(), currentCount + 1);
    // }

    // for (var selector : temp.keySet()) {
    //   var top =
    //       Collections.max(
    //           temp.get(selector),
    //           (c1, c2) ->
    //               scopeCount.getOrDefault(c1.i.selector, 0)
    //                   - scopeCount.getOrDefault(c2.i.selector, 0));
    //   best.put(selector, top);
    // }

    // return best;
  }

  private Map<Selector, List<Candidate>> mostRelevantBySelector(Candidates candidates) {
    var mostRelevantBySelector = new HashMap<Selector, List<Candidate>>();
    for (var selector : candidates.selectors()) {
      mostRelevantBySelector.put(selector, selectMostRelevant(candidates.getFor(selector)));
    }

    return mostRelevantBySelector;
  }

  private List<Candidate> selectMostRelevant(List<Candidate> candidates) {
    var source = mostRelevantSource(candidates);
    return CandidateFilters.of(source).filter(candidates);
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
