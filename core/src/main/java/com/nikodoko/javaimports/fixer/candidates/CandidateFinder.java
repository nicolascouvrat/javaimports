package com.nikodoko.javaimports.fixer.candidates;

import com.nikodoko.javaimports.common.ImportProvider;
import com.nikodoko.javaimports.common.Selector;

public class CandidateFinder {
  public void add(Candidate.Source source, ImportProvider... providers) {}

  public Candidates find(Selector selector) {
    return Candidates.EMPTY;
  }
}
