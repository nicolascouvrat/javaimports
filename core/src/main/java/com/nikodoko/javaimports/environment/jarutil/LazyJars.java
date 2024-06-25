package com.nikodoko.javaimports.environment.jarutil;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Import;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class LazyJars implements JarLoader {
  private final List<LazyJar> jars;

  public LazyJars(Collection<Path> jarPaths) {
    this.jars = jarPaths.stream().map(LazyJar::new).toList();
  }

  @Override
  public Optional<ClassEntity> loadClass(Import i) {
    for (var j : jars) {
      var maybeClass = j.findClass(i);
      if (maybeClass.isPresent()) {
        return maybeClass;
      }
    }

    return Optional.empty();
  }
}
