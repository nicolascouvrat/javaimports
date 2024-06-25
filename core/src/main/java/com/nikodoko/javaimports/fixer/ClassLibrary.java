package com.nikodoko.javaimports.fixer;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.ClassProvider;
import com.nikodoko.javaimports.common.Import;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class ClassLibrary {
  private final List<ClassProvider> providers;

  ClassLibrary() {
    providers = new ArrayList<>();
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
