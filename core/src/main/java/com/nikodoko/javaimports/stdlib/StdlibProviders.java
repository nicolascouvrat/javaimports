package com.nikodoko.javaimports.stdlib;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.stdlib.internal.api.v8.Java8Stdlib;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class StdlibProviders {
  private static class EmptyStdlibProvider implements StdlibProvider {
    @Override
    public Optional<ClassEntity> findClass(Import i) {
      return Optional.empty();
    }

    @Override
    public boolean isInJavaLang(String identifier) {
      return false;
    }

    @Override
    public Collection<Import> findImports(Identifier i) {
      return List.of();
    }
  }

  public static StdlibProvider empty() {
    return new EmptyStdlibProvider();
  }

  public static StdlibProvider java8() {
    return new BasicStdlibProvider(new Java8Stdlib());
  }
}
