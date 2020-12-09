package com.nikodoko.javaimports.fixer.candidates;

import static java.nio.channels.spi.AsynchronousChannelProvider.provider;

import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.ImportProvider;
import com.nikodoko.javaimports.common.Selector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CandidateFinder {
  private Map<Candidate.Source, List<ImportProvider>> providers = new HashMap<>();

  public void add(Candidate.Source source, ImportProvider... additional) {
    var current = providers.computeIfAbsent(source, __ -> new ArrayList<>());
    Collections.addAll(current, additional);
  }

  public Candidates find(Selector selector) {
    var candidates = allFor(selector.identifier());
    if (candidates.size() == 0) {
      return Candidates.EMPTY;
    }

    return Candidates.forSelector(selector).add(allFor(selector.identifier())).build();
  }

  private List<Candidate> allFor(Identifier identifier) {
    var all = new ArrayList<Candidate>();
    for (var entry : providers.entrySet()) {
      entry.getValue().stream()
          .flatMap(provider -> provider.findImports(identifier).stream())
          .map(i -> new Candidate(i, entry.getKey()))
          .forEach(all::add);
    }

    return all;
  }
}
