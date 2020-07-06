package com.nikodoko.javaimports.stdlib;

import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.stdlib.internal.api.v8.Java8Stdlib;
import java.util.HashMap;
import java.util.Map;

public class StdlibProviders {
  private static class EmptyStdlibProvider implements StdlibProvider {
    Map<String, Import> EMPTY_MAP = new HashMap<>();

    @Override
    public Map<String, Import> find(Iterable<String> identifiers) {
      return EMPTY_MAP;
    }

    @Override
    public boolean isInJavaLang(String identifier) {
      return false;
    }
  }

  public static StdlibProvider empty() {
    return new EmptyStdlibProvider();
  }

  public static StdlibProvider java8() {
    return new BasicStdlibProvider(new Java8Stdlib());
  }
}
