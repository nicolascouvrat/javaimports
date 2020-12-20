package com.nikodoko.javaimports.fixer.candidates;

import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.ImportProvider;
import com.nikodoko.javaimports.common.Selector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CandidateFinder {
  private Map<Candidate.Source, List<ImportProvider>> providers = new HashMap<>();

  public void add(Candidate.Source source, ImportProvider... additional) {
    var current = providers.computeIfAbsent(source, __ -> new ArrayList<>());
    Collections.addAll(current, additional);
  }

  public Candidates find(Selector selector) {
    var candidates =
        findForIdentifier(selector.identifier())
            .filter(c -> matchesSelector(c, selector))
            .map(c -> truncate(c, selector))
            .collect(Collectors.toList());
    if (candidates.size() == 0) {
      return Candidates.EMPTY;
    }

    return Candidates.forSelector(selector).add(candidates).build();
  }

  // A candidate matches a selector if the underlying import's selector ends with it.
  private boolean matchesSelector(Candidate candidate, Selector selector) {
    return candidate.i.selector.endsWith(selector);
  }

  private Candidate truncate(Candidate candidate, Selector selector) {
    var importForSelector =
        new Import(candidate.i.selector.subtract(selector), candidate.i.isStatic);
    return new Candidate(importForSelector, candidate.s);
  }

  private Stream<Candidate> findForIdentifier(Identifier identifier) {
    return providers.entrySet().stream()
        .flatMap(e -> generateCandidates(e.getKey(), e.getValue(), identifier));
  }

  private Stream<Candidate> generateCandidates(
      Candidate.Source source, List<ImportProvider> providers, Identifier identifier) {
    return providers.stream()
        .flatMap(p -> p.findImports(identifier).stream())
        .map(i -> new Candidate(i, source));
  }
}
