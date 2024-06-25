package com.nikodoko.javaimports.parser.internal;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Traverse a {@link Scope} tree and extracts the reachable {@link ClassEntity} along with their
 * corresponding {@link Import}. Also stores unreachable classes for observability purposes,
 * although those won't be made visible outside of the parser module.
 */
public record Classes(Map<Import, ClassEntity> reachable, Set<ClassEntity> unreachable) {
  public static Classes of(Scope topScope, Selector pkg) {
    return topScope.childScopes.stream()
        .map(s -> analyze(s, pkg))
        .reduce(new Classes(Map.of(), Set.of()), Classes::merge);
  }

  private static Classes analyze(Scope scope, Selector importPath) {
    Selector thisSelector = null;
    ClassEntity thisClass = null;
    if (scope.maybeClass.isPresent()) {
      var decl = scope.maybeClass.get();
      thisClass =
          ClassEntity.named(decl.name())
              .extending(decl.maybeParent())
              .declaring(Set.copyOf(scope.declarations))
              .build();

      // An anonymous class is never reachable
      if (importPath != null && !decl.name().equals(ClassEntity.ANONYMOUS_CLASS_NAME)) {
        thisSelector = importPath.combine(decl.name());
      }
    }

    var classes = new Classes(Map.of(), Set.of());
    if (thisClass != null && thisSelector != null) {
      classes = new Classes(Map.of(new Import(thisSelector, false), thisClass), Set.of());
    }

    if (thisClass != null && thisSelector == null) {
      classes = new Classes(Map.of(), Set.of(thisClass));
    }

    var nextImportPath = thisSelector;
    return scope.childScopes.stream()
        .map(s -> analyze(s, nextImportPath))
        .reduce(classes, Classes::merge);
  }

  private static Classes merge(Classes a, Classes b) {
    var reachable =
        Stream.concat(a.reachable().entrySet().stream(), b.reachable().entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    var unreachable =
        Stream.concat(a.unreachable().stream(), b.unreachable().stream())
            .collect(Collectors.toSet());
    return new Classes(reachable, unreachable);
  }
}
