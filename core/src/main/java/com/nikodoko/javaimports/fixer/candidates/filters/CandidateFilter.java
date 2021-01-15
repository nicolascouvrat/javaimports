package com.nikodoko.javaimports.fixer.candidates.filters;

import com.nikodoko.javaimports.fixer.candidates.Candidate;
import java.util.List;

public interface CandidateFilter {
  List<Candidate> filter(List<Candidate> candidates);
}
