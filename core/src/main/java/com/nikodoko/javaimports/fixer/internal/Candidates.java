package com.nikodoko.javaimports.fixer.internal;

import com.nikodoko.javaimports.parser.Import;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Candidates {
  private static class Candidate {
    int priority;
    Import i;
  }

  private Map<String, Candidate> candidates = new HashMap<>();

  public void add(int priority, Iterable<Import> imports) {
    for (Import i : imports) {
      add(priority, i);
    }
  }

  public void add(int priority, Import i) {
    Candidate current = candidates.get(i.name());
    if (current == null || current.priority < priority) {
      Candidate better = new Candidate();
      better.i = i;
      better.priority = priority;
      candidates.put(i.name(), better);
    }
  }

  public Optional<Import> get(String identifier) {
    Candidate candidate = candidates.get(identifier);
    if (candidate == null) {
      return Optional.empty();
    }

    return Optional.of(candidate.i);
  }
}
