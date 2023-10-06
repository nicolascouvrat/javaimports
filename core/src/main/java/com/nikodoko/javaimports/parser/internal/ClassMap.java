package com.nikodoko.javaimports.parser.internal;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Traverse a {@link Scope} tree and extracts the reachable {@link ClassEntity} along with their
 * corresponding {@link Import}.
 */
public class ClassMap {
  public static Map<Import, ClassEntity> of(Scope topScope, Selector pkg) {
    var result = new HashMap<Import, ClassEntity>();
    topScope.childScopes.stream()
        .filter(s -> s.maybeClass.isPresent())
        .forEach(s -> result.putAll(getReachableClasses(s, pkg)));

    return result;
  }

  private static Map<Import, ClassEntity> getReachableClasses(Scope scope, Selector path) {
    // We're not expecting to be processing non-class scope
    var decl = scope.maybeClass.orElseThrow(() -> new IllegalStateException("not a class"));
    var result = new HashMap<Import, ClassEntity>();

    // All class imports are non-static
    var i = new Import(path.combine(decl.name()), false);
    var entity =
        ClassEntity.named(decl.name())
            .extending(decl.maybeParent())
            .declaring(Set.copyOf(scope.declarations))
            .build();
    result.put(i, entity);

    scope.childScopes.stream()
        .filter(s -> s.maybeClass.isPresent())
        .forEach(s -> result.putAll(getReachableClasses(s, i.selector)));

    return result;
  }
}
