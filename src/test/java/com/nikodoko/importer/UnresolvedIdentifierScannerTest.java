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
        // Test that we handle scoping correctly in methods
        {
          "class Test {",
          "  public void g() {",
          "    int c = f(b);",
          "  }",
          "  public int f(int a) {",
          "    int b = 2;",
          "    return a + b;",
          "  }",
          "}",
        },
        {"b"},
      },
      {
        {
          // Test that we handle scoping correctly in for loops
          "class Test {",
          "  public void f() {",
          "    for (int i = 0; i < 10; i ++) {",
          "      int b = 2;",
          "      staticFunction(i + b);",
          "    }",
          "    int var = i + b;",
          "    boolean[] c = {true, false};",
          "    for (boolean d : c) {",
          "      boolean e = d;",
          "    }",
          "    boolean f = e || d;",
          "  }",
          "}",
        },
        {"staticFunction", "i", "b", "e", "d"},
      },
      {
        {
          // Test that we handle scoping correctly in if blocks
          "class Test {",
          "  public void f() {",
          "    if (true) {",
          "      int a = 2;",
          "      int b = 3;",
          "    } else {",
          "      int c = a;",
          "    }",
          "    int var = b + c;",
          "  }",
          "}",
        },
        {"a", "b", "c"},
      },
      {
        {
          // Test that we handle scoping correctly in while loops
          "class Test {",
          "  public void f() {",
          "    while (true) {",
          "      int a = 2;",
          "    }",
          "    int var = a;",
          "  }",
          "}",
        },
        {"a"},
      },
      {
        {
          // Test that we handle scoping correctly in do-while loops
          "class Test {",
          "  public void f() {",
          "    do {",
          "      int a = 2;",
          "    } while (true);",
          "    int var = a;",
          "  }",
          "}",
        },
        {"a"},
      },
      {
        {
          // Test that we handle scoping correctly in switch blocks
          "class Test {",
          "  public void f() {",
          "    int a = 2;",
          "    switch (a) {",
          "    case 1:",
          "      int b = 2;",
          "      break;",
          "    case 2:",
          "      int c = b;",
          "      break;",
          "    }",
          "    int var = c;",
          "  }",
          "}",
        },
        {"c"},
      },
      {
        {
          // Test that we handle scoping correctly in try-catch-finally
          "class Test {",
          "  public void f() {",
          "    try {",
          "      int a = 1;",
          "    } catch (SomeException e) {",
          "      int b = e.getErrorCode();",
          "    } catch (Exception e) {",
          "      int c = a;",
          "    } finally {",
          "      int d = b;",
          "    }",
          "    int var = c + e;",
          "  }",
          "}",
        },
        {"SomeException", "Exception", "a", "b", "c", "e"},
      },
      {
        {
          // Test that we handle scoping correctly in try-catch-finally (with resource)
          "class Test {",
          "  public void f() {",
          "    try (int r = 1) {",
          "      int a = 1 + r;",
          "    } catch (SomeException e) {",
          "      int b = e.getErrorCode();",
          "    } catch (Exception e) {",
          "      int c = a + r;",
          "    } finally {",
          "      int d = b + r;",
          "    }",
          "    int var = c + e + r;",
          "  }",
          "}",
        },
        {"SomeException", "Exception", "a", "b", "c", "e", "r"},
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
    assertThat(scanner.unresolved()).containsExactlyElementsIn(expected);
  }
}
