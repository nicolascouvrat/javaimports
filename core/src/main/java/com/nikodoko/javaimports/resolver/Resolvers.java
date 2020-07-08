package com.nikodoko.javaimports.resolver;

import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class Resolvers {
  private static class DummyResolver implements Resolver {
    @Override
    public Optional<Import> find(String identifier) {
      return Optional.empty();
    }

    @Override
    public Set<ParsedFile> filesInPackage(String packageName) {
      return new HashSet<>();
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
