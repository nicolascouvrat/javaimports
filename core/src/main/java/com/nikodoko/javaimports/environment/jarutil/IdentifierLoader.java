package com.nikodoko.javaimports.environment.jarutil;

import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

@FunctionalInterface
public interface IdentifierLoader {
  @FunctionalInterface
  public interface Factory {
    public IdentifierLoader of(Collection<Path> paths);
  }

  public Set<Identifier> loadIdentifiers(Import i);
}
