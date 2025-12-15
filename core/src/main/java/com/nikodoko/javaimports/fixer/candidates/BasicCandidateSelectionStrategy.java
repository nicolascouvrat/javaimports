package com.nikodoko.javaimports.fixer.candidates;

import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.fixer.candidates.filters.CandidateFilters;
import java.util.Collection;
import java.util.logging.Logger;

public class BasicCandidateSelectionStrategy implements CandidateSelectionStrategy {
  private static Logger log = Logs.getLogger(BasicCandidateSelectionStrategy.class.getName());

  private final Selector pkg;
  private final Collection<Import> currentImports;

  public BasicCandidateSelectionStrategy(Selector pkg, Collection<Import> currentImports) {
    this.pkg = pkg;
    this.currentImports = currentImports;
  }

  @Override
  public BestCandidates selectBest(Candidates candidates) {
    log.info("candidates: %s".formatted(candidates));
    log.info("existing: %s".formatted(currentImports));
    var filtered = bestSource(candidates);
    filtered = mostCommonScope(filtered);
    filtered = sourceSpecificRules(filtered);
    return takeFirst(filtered);
  }

  private Candidates bestSource(Candidates candidates) {
    return CandidateFilters.mostRelevantSource().filter(candidates);
  }

  private Candidates mostCommonScope(Candidates candidates) {
    return CandidateFilters.mostCommonScope(currentImports).filter(candidates);
  }

  private Candidates sourceSpecificRules(Candidates candidates) {
    return CandidateFilters.sourceSpecificRules(pkg).filter(candidates);
  }

  private BestCandidates takeFirst(Candidates candidates) {
    return new TakeFirstCandidateSelectionStrategy().selectBest(candidates);
  }
}
