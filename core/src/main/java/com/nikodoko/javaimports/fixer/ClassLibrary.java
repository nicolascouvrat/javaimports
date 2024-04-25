package com.nikodoko.javaimports.fixer;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.ClassProvider;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class ClassLibrary {
  private static class JavaLangObjectProvider implements ClassProvider {
    private static final JavaLangObjectProvider INSTANCE = new JavaLangObjectProvider();

    private static final Import JAVA_LANG_OBJECT_IMPORT =
        new Import(Selector.JAVA_LANG_OBJECT, false);

    @Override
    public Optional<ClassEntity> findClass(Import i) {
      if (i.equals(JAVA_LANG_OBJECT_IMPORT)) {
        return Optional.of(ClassEntity.JAVA_LANG_OBJECT);
      }

      return Optional.empty();
    }
  }

  private final List<ClassProvider> providers;

  ClassLibrary() {
    providers = new ArrayList<>();
    // providers.add(JavaLangObjectProvider.INSTANCE);
  }

  void add(ClassProvider provider) {
    providers.add(provider);
  }

  Optional<ClassEntity> find(Import i) {
    return providers.stream()
        .map(p -> p.findClass(i))
        .filter(Optional::isPresent)
        .map(Optional::get)
        // TODO: a better way to handle conficts? Although these should be rare because it would
        // mean the import is the exact same
        // the package is the exact same.
        .findFirst();
  }
}
