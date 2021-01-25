package com.nikodoko.javaimports.fixer.candidates;

import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.fixer.candidates.filters.CandidateFilters;
import com.nikodoko.javaimports.fixer.candidates.filters.SourceCandidateFilter;
import com.nikodoko.javaimports.fixer.candidates.filters.StdlibCandidateFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BasicCandidateSelectionStrategy implements CandidateSelectionStrategy {
  @Override
  public BestCandidates selectBest(Candidates candidates) {
    var filtered = bestSource(candidates);
    filtered = mostCommonScope(filtered);
    filtered = sourceSpecificRules(filtered);
    return takeFirst(filtered);

    // var builder = BestCandidates.builder();
    // for (var entry : bestImportBySelector(candidates).entrySet()) {
    //   builder.put(entry.getKey(), entry.getValue());
    // }

    // return builder.build();
  }

  private Map<Selector, Import> bestImportBySelector(Candidates candidates) {
    var mostRelevantBySelector = mostRelevantBySelector(candidates);
    var bestBySelector = new HashMap<Selector, Import>();
    for (var selector : mostRelevantBySelector.keySet()) {
      if (mostRelevantBySelector.get(selector).isEmpty()) {
        throw new IllegalStateException(
            String.format("Why do we have empty imports for selector=%s ??", selector));
      }

      if (mostRelevantBySelector.get(selector).size() == 1) {
        bestBySelector.put(selector, mostRelevantBySelector.get(selector).get(0).i);
      }
    }

    var scopeCount = new HashMap<Selector, Integer>();
    for (var i : bestBySelector.values()) {
      var currentCount = scopeCount.getOrDefault(i.selector.scope(), 0);
      scopeCount.put(i.selector.scope(), currentCount + 1);
    }

    for (var selector : mostRelevantBySelector.keySet()) {
      var top =
          Collections.max(
              mostRelevantBySelector.get(selector),
              (c1, c2) ->
                  scopeCount.getOrDefault(c1.i.selector.scope(), 0)
                      - scopeCount.getOrDefault(c2.i.selector.scope(), 0));
      bestBySelector.put(selector, top.i);
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

  private Candidates bestSource(Candidates candidates) {
    var filtered = Candidates.EMPTY;
    for (var selector : candidates.selectors()) {
      var forSelector =
          Candidates.forSelector(selector).add(bestSource(candidates.getFor(selector))).build();
      filtered = Candidates.merge(filtered, forSelector);
    }

    return filtered;
  }

  private List<Candidate> bestSource(List<Candidate> candidates) {
    var source = mostRelevantSource(candidates);
    return new SourceCandidateFilter(source).filter(candidates);
  }

  private Candidates mostCommonScope(Candidates candidates) {
    var decided =
        candidates.selectors().stream()
            .filter(s -> candidates.getFor(s).size() == 1)
            .collect(Collectors.toList());
    var scopeCount = new HashMap<Selector, Integer>();
    for (var selector : decided) {
      var scope = candidates.getFor(selector).get(0).i.selector.scope();
      var currentCount = scopeCount.getOrDefault(scope, 0);
      scopeCount.put(scope, currentCount + 1);
    }

    var filtered = Candidates.EMPTY;
    for (var selector : candidates.selectors()) {
      var top =
          Collections.max(
              candidates.getFor(selector),
              (c1, c2) ->
                  scopeCount.getOrDefault(c1.i.selector.scope(), 0)
                      - scopeCount.getOrDefault(c2.i.selector.scope(), 0));
      var topCount = scopeCount.getOrDefault(top.i.selector.scope(), 0);
      var best =
          candidates.getFor(selector).stream()
              .filter(c -> scopeCount.getOrDefault(c.i.selector.scope(), 0) == topCount)
              .collect(Collectors.toList());
      filtered = Candidates.merge(filtered, Candidates.forSelector(selector).add(best).build());
    }

    return filtered;
  }

  private Candidates sourceSpecificRules(Candidates candidates) {
    var filtered = Candidates.EMPTY;
    for (var selector : candidates.selectors()) {
      var source = candidates.getFor(selector).get(0).s;
      var best = candidates.getFor(selector);
      if (source == Candidate.Source.STDLIB) {
        best = new StdlibCandidateFilter().filter(candidates.getFor(selector));
      }

      filtered = Candidates.merge(filtered, Candidates.forSelector(selector).add(best).build());
    }

    return filtered;
  }

  private BestCandidates takeFirst(Candidates candidates) {
    var best = BestCandidates.builder();
    for (var selector : candidates.selectors()) {
      best.put(selector, candidates.getFor(selector).get(0).i);
    }

    return best.build();
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
