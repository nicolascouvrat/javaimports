package com.nikodoko.javaimports.stdlib;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import com.nikodoko.javaimports.environment.jarutil.IdentifierLoader;
import com.nikodoko.javaimports.environment.jarutil.JarIdentifierLoader;
import com.nikodoko.javaimports.stdlib.internal.Stdlib;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class BasicStdlibProvider implements StdlibProvider {
  private Stdlib stdlib;
  private Map<String, Integer> usedPackages = new HashMap<>();
  // TODO: it is not ideal to rely on a class that was made for jar parsing here, but it provides us
  // with a temporary solution
  private final IdentifierLoader loader = new JarIdentifierLoader(List.of());
  private static final Selector JAVA_LANG = Selector.of("java", "lang");

  public BasicStdlibProvider(Stdlib stdlib) {
    this.stdlib = stdlib;
  }

  @Override
  public boolean isInJavaLang(Identifier identifier) {
    var matches = stdlib.getClassesFor(identifier);
    if (matches == null) {
      return false;
    }

    for (var match : matches) {
      // We don't want to catch classes like java.lang.Thread.State, as those will need to be
      // imported.
      if (match.selector.startsWith(JAVA_LANG) && match.selector.size() == JAVA_LANG.size() + 1) {
        return true;
      }
    }

    return false;
  }

  @Override
  public Collection<Import> findImports(Identifier i) {
    var span = Traces.createSpan("BasicStdlibProvider.findImports", new Tag("identifier", i));
    try (var __ = Traces.activate(span)) {
      return findImportsInstrumented(i);
    } finally {
      span.finish();
    }
  }

  @Override
  public Optional<ClassEntity> findClass(Import i) {
    var span = Traces.createSpan("BasicStdlibProvider.findClass", new Tag("import", i));
    try (var __ = Traces.activate(span)) {
      var c = findClassInstrumented(i);
      Traces.addTags(span, new Tag("class", c));
      return c;
    } catch (Throwable t) {
      Traces.addThrowable(span, t);
      return Optional.empty();
    } finally {
      span.finish();
    }
  }

  private Optional<ClassEntity> findClassInstrumented(Import i) {
    var c = ClassEntity.named(i.selector).declaring(loader.loadIdentifiers(i)).build();
    return Optional.of(c);
  }

  private Collection<Import> findImportsInstrumented(Identifier i) {
    var found = stdlib.getClassesFor(i);
    if (found == null) {
      return List.of();
    }

    return Arrays.stream(found).collect(Collectors.toList());
  }
}
