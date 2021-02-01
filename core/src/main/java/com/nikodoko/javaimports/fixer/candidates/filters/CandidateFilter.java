package com.nikodoko.javaimports.fixer.candidates.filters;

import com.nikodoko.javaimports.fixer.candidates.Candidates;

public interface CandidateFilter {
  Candidates filter(Candidates candidates);
}
