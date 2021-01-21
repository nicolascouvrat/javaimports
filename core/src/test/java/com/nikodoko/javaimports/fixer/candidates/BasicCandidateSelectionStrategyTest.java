package com.nikodoko.javaimports.fixer.candidates;

import static com.google.common.truth.Truth.assertThat;
import static com.nikodoko.javaimports.common.CommonTestUtil.arbitraryImportEndingWith;
import static com.nikodoko.javaimports.common.CommonTestUtil.arbitrarySelector;
import static com.nikodoko.javaimports.common.CommonTestUtil.arbitrarySelectorOfSize;

import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import java.util.Collections;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

public class BasicCandidateSelectionStrategyTest {
  static class SelectorAndImports {
    final Selector selector;
    final List<Import> imports;

    SelectorAndImports(Selector s, List<Import> i) {
      selector = s;
      imports = i;
    }
  }

  @Property
  void stdlibIsMoreRelevantThanExternal(@ForAll("endingWith") SelectorAndImports data) {
    var stdlibCandidate = new Candidate(data.imports.get(0), Candidate.Source.STDLIB);
    var externalCandidate = new Candidate(data.imports.get(1), Candidate.Source.EXTERNAL);
    var candidates =
        Candidates.forSelector(data.selector).add(stdlibCandidate, externalCandidate).build();
    var expected = BestCandidates.builder().put(data.selector, stdlibCandidate.i).build();

    var got = new BasicCandidateSelectionStrategy().selectBest(candidates);

    assertThat(got).isEqualTo(expected);
  }

  @Property
  void siblingsIsMoreRelevantThanStdlibOrExternal(@ForAll("endingWith") SelectorAndImports data) {
    var stdlibCandidate = new Candidate(data.imports.get(0), Candidate.Source.STDLIB);
    var externalCandidate = new Candidate(data.imports.get(1), Candidate.Source.EXTERNAL);
    var siblingCandidate = new Candidate(data.imports.get(2), Candidate.Source.SIBLING);
    var candidates =
        Candidates.forSelector(data.selector)
            .add(stdlibCandidate, externalCandidate, siblingCandidate)
            .build();
    var expected = BestCandidates.builder().put(data.selector, siblingCandidate.i).build();

    var got = new BasicCandidateSelectionStrategy().selectBest(candidates);

    assertThat(got).isEqualTo(expected);
  }

  @Property
  void shorterStdlibPathsAreMoreRelevantThanLongOnes(
      @ForAll("endingWith") SelectorAndImports data) {
    var shortest =
        Collections.min(data.imports, (i1, i2) -> i1.selector.size() - i2.selector.size());
    var candidates =
        Candidates.forSelector(data.selector)
            .add(
                data.imports.stream()
                    .map(i -> new Candidate(i, Candidate.Source.STDLIB))
                    .toArray(Candidate[]::new))
            .build();
    var expected = BestCandidates.builder().put(data.selector, shortest).build();

    var got = new BasicCandidateSelectionStrategy().selectBest(candidates);

    assertThat(got).isEqualTo(expected);
  }

  @Property
  void javaUtilIsMoreRelevantThanStdlibOfSameSize(
      @ForAll("endingWith") SelectorAndImports data, @ForAll("ofSize2") Selector prefix) {
    var stdlibImport =
        new Import(prefix.combine(data.imports.get(0).selector), data.imports.get(0).isStatic);
    var javaUtilImport =
        new Import(
            Selector.of("java", "util").combine(data.imports.get(0).selector),
            data.imports.get(0).isStatic);
    var candidates =
        Candidates.forSelector(data.selector)
            .add(
                new Candidate(stdlibImport, Candidate.Source.STDLIB),
                new Candidate(javaUtilImport, Candidate.Source.STDLIB))
            .build();
    var expected = BestCandidates.builder().put(data.selector, javaUtilImport).build();

    var got = new BasicCandidateSelectionStrategy().selectBest(candidates);

    assertThat(got).isEqualTo(expected);
  }

  @Provide
  Arbitrary<SelectorAndImports> endingWith() {
    return arbitrarySelector()
        .flatMap(
            sel ->
                Combinators.combine(
                        Arbitraries.of(sel), arbitraryImportEndingWith(sel).list().ofSize(10))
                    .as(SelectorAndImports::new));
  }

  @Provide
  Arbitrary<Selector> ofSize2() {
    return arbitrarySelectorOfSize(2, 2);
  }
}
