package com.nikodoko.javaimports.environment.jarutil;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Import;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

@FunctionalInterface
public interface JarLoader {
  @FunctionalInterface
  public interface Factory {
    public JarLoader of(Collection<Path> paths);
  }

  public Optional<ClassEntity> loadClass(Import i);
}
