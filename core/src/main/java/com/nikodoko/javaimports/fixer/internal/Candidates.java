package com.nikodoko.javaimports.fixer.internal;

import com.nikodoko.javaimports.parser.Import;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Candidates {
  /**
   * Relative priorities of candidates: candidates found in siblings take precedence over those
   * found in the stdlib, that then take precedence over external imports (found in third party
   * dependencies).
   */
  public static enum Priority {
    EXTERNAL,
    STDLIB,
    SIBLING;
  }

  private static class Candidate {
    Priority p;
    Import i;
  }

  private Map<String, Candidate> candidates = new HashMap<>();

  public void add(Priority p, Iterable<Import> imports) {
    for (Import i : imports) {
      add(p, i);
    }
  }

  public void add(Priority p, Import i) {
    Candidate current = candidates.get(i.name());
    if (current == null || current.p.compareTo(p) < 0) {
      Candidate better = new Candidate();
      better.i = i;
      better.p = p;
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
