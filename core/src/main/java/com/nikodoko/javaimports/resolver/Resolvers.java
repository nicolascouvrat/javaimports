package com.nikodoko.javaimports.resolver;

import com.nikodoko.javaimports.parser.Import;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

  public static Resolver basedOnEnvironment(Path filename) {
    Path current = filename.getParent();
    while (current != null) {
      Path potentialPom = Paths.get(current.toString(), "pom.xml");
      if (Files.exists(potentialPom)) {
        return new MavenResolver(current, filename);
      }

      current = current.getParent();
    }

    return new DummyResolver();
  }
}
