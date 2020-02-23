package com.nikodoko.importer;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.sun.tools.javac.util.Context;
import java.util.Collection;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class UnresolvedIdentifierScannerTest {
  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> parameters() {
    String[][][] inputOutputs = {
      {
        {
          "class Test {",
          "  public static void main(String[] args) {",
          "    A.staticFunction(args);",
          "  }",
          "}",
        },
        // String will pop up as it is in the universe scope, which UnresolvedIdentifierScanner does
        // not know about
        {"A", "String"},
      },
    };
    ImmutableList.Builder<Object[]> builder = ImmutableList.builder();
    for (String[][] inputOutput : inputOutputs) {
      String input = String.join("\n", inputOutput[0]) + "\n";
      Set<String> output = Sets.newHashSet(inputOutput[1]);
      Object[] parameters = {input, output};
      builder.add(parameters);
    }
    return builder.build();
  }

  private final String input;
  private final Set<String> expected;

  public UnresolvedIdentifierScannerTest(String input, Set<String> expected) {
    this.input = input;
    this.expected = expected;
  }

  @Test
  public void scanTest() throws Exception {
    UnresolvedIdentifierScanner scanner = new UnresolvedIdentifierScanner();
    scanner.scan(ImportFixer.parse(new Context(), input), null);
    assertThat(scanner.unresolved()).isEqualTo(expected);
  }
}
