package com.nikodoko.javaimports.resolver;

import com.nikodoko.javaimports.parser.Import;
import java.util.Optional;

public class Resolvers {
  private static class DummyResolver implements Resolver {
    @Override
    public Optional<Import> find(String identifier) {
      return Optional.empty();
    }
  }

  public static Resolver empty() {
    return new DummyResolver();
  }
}
