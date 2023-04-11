package com.nikodoko.javaimports.parser;

import static com.google.common.truth.Truth.assertThat;
import static com.nikodoko.javaimports.common.CommonTestUtil.aSelector;
import static com.nikodoko.javaimports.common.CommonTestUtil.anImport;
import static com.nikodoko.javaimports.common.CommonTestUtil.someIdentifiers;
import static org.junit.Assert.fail;

import com.google.common.truth.IterableSubject;
import com.nikodoko.javaimports.ImporterException;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.ClassDeclaration;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Superclass;
import com.nikodoko.javaimports.parser.internal.Scope;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class OrphansTest {
  @Test
  void itShouldNotHideYoungerOrphanIfOlderOrphanParentFound() {
    var code =
        """
      package pkg.com.test;
      class Test {
        static class Child extends Parent {
          static class OtherChild extends AChild {
            public void m() {
              int c = n(f() + g(0));
            }
          }
        }

        static class Parent {
          protected int a = 0;
          public int p(int x) {
            return x;
          }
          public int g(int x) {
            int b = 5;
            return x;
          }
          int h(int x) {
            return x;
          }
        }
      }
    """;

    var orphans = getOrphans(code);
    // Before the first traversal, all symbols in orphan classes are unresolved
    assertThat(orphans.unresolved()).containsExactlyElementsIn(someIdentifiers("f", "g", "n"));
    var expected =
        new ClassDeclaration(aSelector("OtherChild"), Superclass.unresolved(aSelector("AChild")));
    assertThatOrphans(orphans).containsExactly(expected);
    // After traversal, we still have an orphan class, so its symbols are still unresolved, but the
    // ones provided by its parent are resolved
    assertThat(orphans.unresolved()).containsExactlyElementsIn(someIdentifiers("f", "n"));
    assertThat(orphans.needsParents()).isTrue();
  }

  @Test
  void itShouldHideYoungerOrphanIfOlderOrphanParentNotFound() {
    var code =
        """
      package pkg.com.test;
      class Test {
        static class Child extends Parent {
          static class OtherChild extends AChild {
            public void m() {
              int c = n(f() + g(0));
            }
          }
        }
      }
    """;

    var orphans = getOrphans(code);
    // Before the first traversal, all symbols in orphan classes are unresolved
    assertThat(orphans.unresolved()).containsExactlyElementsIn(someIdentifiers("f", "g", "n"));
    var expected =
        new ClassDeclaration(aSelector("Child"), Superclass.unresolved(aSelector("Parent")));
    assertThatOrphans(orphans).containsExactly(expected);
    // After traversal, we still have an orphan class, so its symbols are still unresolved
    assertThat(orphans.unresolved()).containsExactlyElementsIn(someIdentifiers("f", "g", "n"));
    assertThat(orphans.needsParents()).isTrue();
  }

  @Test
  void itShouldHideIfParentFoundIsChildOfOrphan() {
    var code =
        """
      package pkg.com.test;
      class Test {
        static class Child extends Parent {
          static class OtherChild extends External {
            public void m() {
              int c = n(f() + g(0));
            }
          }
        }

        static class Other extends Child.OtherChild {}
      }
    """;

    var orphans = getOrphans(code);
    // Before the first traversal, all symbols in orphan classes are unresolved
    assertThat(orphans.unresolved()).containsExactlyElementsIn(someIdentifiers("f", "g", "n"));
    var expected =
        new ClassDeclaration(aSelector("Child"), Superclass.unresolved(aSelector("Parent")));
    assertThatOrphans(orphans).containsExactly(expected);
    // After traversal, we still have an orphan class, so its symbols are still unresolved
    assertThat(orphans.unresolved()).containsExactlyElementsIn(someIdentifiers("f", "g", "n"));
    assertThat(orphans.needsParents()).isTrue();
  }

  @Test
  void itShouldRedoLocalResolutionAfterBeingSuppliedMoreInfo() {
    var code =
        """
      package pkg.com.test;
      class Test {
        static class Other extends Child.OtherChild {
          public void x() {
            m();
          }
        }

        static class Child extends Parent {
          public void y() {
            z();
          }

          static class OtherChild extends External {
            public void m() {
              int c = n(f() + g(0));
            }
          }
        }
      }
    """;

    var orphans = getOrphans(code);
    // Before the first traversal, all symbols in orphan classes are unresolved
    assertThat(orphans.unresolved())
        .containsExactlyElementsIn(someIdentifiers("f", "g", "n", "m", "z"));
    var traverser = orphans.traverse();
    var next = traverser.next();
    assertThat(next)
        .isEqualTo(
            new ClassDeclaration(aSelector("Child"), Superclass.unresolved(aSelector("Parent"))));
    var parent = ClassEntity.named(aSelector("Parent")).declaring(someIdentifiers("z")).build();
    traverser.addParent(anImport("whatever.Parent"), parent);
    // Some unresolved symbols went away at this point
    assertThat(orphans.unresolved()).containsExactlyElementsIn(someIdentifiers("f", "g", "n", "m"));

    var expected =
        List.of(
            new ClassDeclaration(aSelector("Other"), Superclass.unresolved(aSelector("External"))),
            new ClassDeclaration(
                aSelector("OtherChild"), Superclass.unresolved(aSelector("External"))));
    assertThatOrphans(orphans).containsExactlyElementsIn(expected);
    // Some more unresolved symbols went away at this point
    assertThat(orphans.unresolved()).containsExactlyElementsIn(someIdentifiers("f", "g", "n"));
    assertThat(orphans.needsParents()).isTrue();
  }

  @Test
  void itShouldResolveInnerOrphansParent() {
    var code =
        """
      package pkg.com.test;
      class Test extends Parent{
        static class Inner extends OtherParent {
        }
      }
    """;

    var orphans = getOrphans(code);
    var traverser = orphans.traverse();
    var next = traverser.next();
    assertThat(next)
        .isEqualTo(
            new ClassDeclaration(aSelector("Test"), Superclass.unresolved(aSelector("Parent"))));

    var parent =
        ClassEntity.named(aSelector("Parent")).declaring(someIdentifiers("OtherParent")).build();
    traverser.addParent(anImport("com.mycompany.Parent"), parent);

    assertThatOrphans(orphans)
        .containsExactly(
            new ClassDeclaration(
                aSelector("Inner"),
                Superclass.resolved(anImport("com.mycompany.Parent.OtherParent"))));
    assertThat(orphans.needsParents()).isTrue();
  }

  @Test
  void itShouldHaveOrphansIfNotAllParentsFoundLocally() {
    var code =
        """
      package pkg.com.test;
      class Test {
        static class Child extends Parent {
          void f() {
            int c = g(a) + h(b);
          }
        }

        static class OtherChild extends AChild {
          public void m() {
            int c = n(f() + g(0));
          }
        }

        static class Parent {
          protected int a = 0;
          public int p(int x) {
            return x;
          }
          public int g(int x) {
            int b = 5;
            return x;
          }
          int h(int x) {
            return x;
          }
        }
      }
    """;

    var orphans = getOrphans(code);
    assertThat(orphans.unresolved())
        .containsExactlyElementsIn(someIdentifiers("f", "g", "n", "h", "a", "b"));
    var expected =
        new ClassDeclaration(aSelector("OtherChild"), Superclass.unresolved(aSelector("AChild")));
    assertThatOrphans(orphans).containsExactly(expected);
    assertThat(orphans.unresolved()).containsExactlyElementsIn(someIdentifiers("f", "n", "g", "b"));
    assertThat(orphans.needsParents()).isTrue();
  }

  @Test
  void itShouldHaveNoOrphansIfAllParentsFoundLocally() {
    var code =
        """
      package pkg.com.test;
      class Test {
        static class Child extends Parent {
          void f() {
            int c = g(a) + h(b);
          }
        }

        static class OtherChild extends Child {
          public void m() {
            int c = n(f() + g(0));
          }
        }

        static class Parent {
          protected int a = 0;
          public int p(int x) {
            return x;
          }
          public int g(int x) {
            int b = 5;
            return x;
          }
          int h(int x) {
            return x;
          }
        }
      }
    """;

    var orphans = getOrphans(code);
    assertThat(orphans.unresolved())
        .containsExactlyElementsIn(someIdentifiers("f", "g", "n", "h", "a", "b"));
    assertThatOrphans(orphans).isEmpty();
    assertThat(orphans.unresolved()).containsExactlyElementsIn(someIdentifiers("b", "n"));
    assertThat(orphans.needsParents()).isFalse();
  }

  record OrphansAndScope(Orphans orphans, Scope topScope) implements Orphans {
    public Set<Identifier> unresolved() {
      Set<Identifier> collector = new HashSet<>();
      collectUnresolved(collector, topScope);
      return collector;
    }

    private static void collectUnresolved(Set<Identifier> collector, Scope scope) {
      collector.addAll(scope.unresolved);
      for (var s : scope.childScopes) {
        collectUnresolved(collector, s);
      }
    }

    @Override
    public Orphans.Traverser traverse() {
      return orphans.traverse();
    }

    @Override
    public boolean needsParents() {
      return orphans.needsParents();
    }
  }

  private IterableSubject assertThatOrphans(Orphans orphans) {
    var allOrphans = new ArrayList<ClassDeclaration>();
    var traverser = orphans.traverse();
    var next = traverser.next();
    while (next != null) {
      allOrphans.add(next);
      next = traverser.next();
    }

    return assertThat(allOrphans);
  }

  private OrphansAndScope getOrphans(String code) {
    var parser = new Parser(Options.defaults());
    try {
      var scope = parser.parse(Paths.get(""), code).get().topScope();
      return new OrphansAndScope(Orphans.wrapping(scope), scope);
    } catch (ImporterException e) {
      for (ImporterException.ImporterDiagnostic d : e.diagnostics()) {
        System.out.println(d);
      }
      fail();
    }

    return null;
  }
}
