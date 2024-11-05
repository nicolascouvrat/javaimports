package com.nikodoko.javaimports.parser.internal;

import com.nikodoko.javaimports.common.ClassDeclaration;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Utils;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Maintains the set of identifiers delared in the scope, as well as a link to the immediately
 * surrounding (parent) scope.
 *
 * <p>Also maintains a set of unresolved identifiers found in this scope.
 *
 * <p>The top level scope should have {@code parent==null}.
 */
public class Scope {
  // Populated if this scope is a class scope
  public Optional<ClassDeclaration> maybeClass = Optional.empty();

  public Set<Identifier> declarations = new HashSet<>();
  public Set<Identifier> unresolved = new HashSet<>();

  public List<Scope> childScopes = new ArrayList<>();
  public Scope parent = null;

  public void addChild(Scope scope) {
    childScopes.add(scope);
  }

  public void declare(Identifier identifier) {
    declarations.add(identifier);
    resolve(identifier);
  }

  private void resolve(Identifier identifier) {
    unresolved.remove(identifier);
    for (var scope : childScopes) {
      scope.resolve(identifier);
    }
  }

  public void maybeAddUnresolved(Identifier identifier) {
    if (resolvable(identifier) || JavaKeywords.ALL.contains(identifier)) {
      return;
    }

    unresolved.add(identifier);
  }

  private boolean resolvable(Identifier identifier) {
    var inCurrentScope = declarations.contains(identifier);
    if (inCurrentScope || parent == null) {
      return inCurrentScope;
    }

    return parent.resolvable(identifier);
  }

  public Set<Identifier> identifiers = new HashSet<>();

  /**
   * Debugging support.
   *
   * <p>This does not output anything about the parent scopes.
   */
  public String toString() {
    return Utils.toStringHelper(this)
        .add("declarations", declarations)
        .add("unresolved", unresolved)
        .add("maybeClass", maybeClass)
        .toString();
  }

  public static void debugPrintScopeTree(Scope scope, PrintStream ps) {
    debugPrintScopeTreeLevel(0, scope, ps);
  }

  private static void debugPrintScopeTreeLevel(int level, Scope scope, PrintStream ps) {
    var prefix = " ".repeat(2 * level);
    ps.print(prefix);
    ps.println(scope);
    for (var cs : scope.childScopes) {
      debugPrintScopeTreeLevel(level + 1, cs, ps);
    }
  }
}
