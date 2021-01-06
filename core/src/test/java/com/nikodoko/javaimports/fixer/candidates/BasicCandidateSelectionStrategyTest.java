package com.nikodoko.javaimports.fixer.candidates;

import static com.google.common.truth.Truth.assertThat;

import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import org.junit.jupiter.api.Test;

public class BasicCandidateSelectionStrategyTest {
  @Test
  void itShouldSelectCandidatesAccordingToSource() {
    var candidates =
        Candidates.merge(
            Candidates.forSelector(Selector.of("A"))
                .add(
                    new Candidate(
                        new Import(Selector.of("com", "pkg", "A"), false), Candidate.Source.STDLIB),
                    new Candidate(
                        new Import(Selector.of("com", "another", "pkg", "A"), false),
                        Candidate.Source.SIBLING))
                .build(),
            Candidates.forSelector(Selector.of("B"))
                .add(
                    new Candidate(
                        new Import(Selector.of("com", "pkg", "B"), false), Candidate.Source.STDLIB),
                    new Candidate(
                        new Import(Selector.of("com", "another", "pkg", "B"), false),
                        Candidate.Source.EXTERNAL))
                .build());

    var expected =
        BestCandidates.builder()
            .put(Selector.of("A"), new Import(Selector.of("com", "another", "pkg", "A"), false))
            .put(Selector.of("B"), new Import(Selector.of("com", "pkg", "B"), false))
            .build();
    var got = new BasicCandidateSelectionStrategy().selectBest(candidates);
    assertThat(got).isEqualTo(expected);
  }
}
