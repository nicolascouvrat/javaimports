package com.nikodoko.javaimports.stdlib;

import com.nikodoko.javaimports.parser.Import;
import java.util.HashMap;
import java.util.Map;

public class StdlibProviders {
  private static class EmptyStdlibProvider implements StdlibProvider {
    @Override
    public Map<String, Import> find(Iterable<String> identifiers) {
      return new HashMap<>();
    }
  }

  public static StdlibProvider empty() {
    return new EmptyStdlibProvider();
  }
}
