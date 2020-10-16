package com.nikodoko.javaimports.fixer.internal;

import static com.google.common.truth.Truth8.assertThat;

import com.nikodoko.javaimports.parser.Import;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CandidatesTest {
  Candidates candidates;

  @BeforeEach
  void setup() {
    candidates = new Candidates();
  }

  @Test
  void testHigherPriorityOverwritesLower() {
    candidates.add(1, new Import("List", "java.util", false));
    candidates.add(2, new Import("List", "a.dependency", false));

    assertThat(candidates.get("List")).hasValue(new Import("List", "a.dependency", false));
  }

  @Test
  void testLowerPriorityDoesNotOverwriteHigher() {
    candidates.add(2, new Import("List", "a.dependency", false));
    candidates.add(1, new Import("List", "java.util", false));

    assertThat(candidates.get("List")).hasValue(new Import("List", "a.dependency", false));
  }
}
