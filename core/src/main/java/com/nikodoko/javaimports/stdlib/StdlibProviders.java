package com.nikodoko.javaimports.stdlib;

import com.google.common.collect.ImmutableMap;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.stdlib.internal.Java0Stdlib;
import com.nikodoko.javaimports.stdlib.internal.Java8Stdlib;
import java.util.HashMap;
import java.util.Map;

public class StdlibProviders {
  private static class DummyStdlibProvider implements StdlibProvider {
    Map<String, Import> stdlib;

    DummyStdlibProvider(Map<String, Import> stdlib) {
      this.stdlib = stdlib;
    }

    @Override
    public Map<String, Import> find(Iterable<String> identifiers) {
      Map<String, Import> found = new HashMap<>();
      for (String identifier : identifiers) {
        Import match = stdlib.get(identifier);
        if (match == null) {
          continue;
        }

        found.put(identifier, match);
      }

      return found;
    }

    @Override
    public boolean isInJavaLang(String identifier) {
      Import found = stdlib.get(identifier);
      if (found == null) {
        return false;
      }

      return found.qualifier().startsWith("java.lang");
    }
  }

  public static StdlibProvider empty() {
    return new DummyStdlibProvider(new HashMap<>());
  }

  public static StdlibProvider stub() {
    // FIXME:remove this
    return new DummyStdlibProvider(
        ImmutableMap.of(
            "List",
            new Import("List", "java.util", false),
            "ArrayList",
            new Import("ArrayList", "java.util", false)));
  }

  public static StdlibProvider java0() {
    return new BasicStdlibProvider(new Java0Stdlib());
  }

  public static StdlibProvider java8() {
    return new BasicStdlibProvider(new Java8Stdlib());
  }
}
