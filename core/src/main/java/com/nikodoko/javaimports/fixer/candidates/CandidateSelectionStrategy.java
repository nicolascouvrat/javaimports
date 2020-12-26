package com.nikodoko.javaimports.fixer.candidates;

import com.nikodoko.javaimports.common.Import;
import java.util.Collection;

public interface CandidateSelectionStrategy {
  /**
   * Select the best candidate for each selector in {@code candidates}, maybe using the {@code
   * current} imports to refine that selection.
   */
  BestCandidates selectBest(Candidates candidates, Collection<Import> current);
}
