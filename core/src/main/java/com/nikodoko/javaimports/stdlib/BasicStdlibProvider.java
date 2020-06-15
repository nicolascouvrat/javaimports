package com.nikodoko.javaimports.stdlib;

import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.stdlib.internal.Stdlib;
import java.util.HashMap;
import java.util.Map;

public class BasicStdlibProvider implements StdlibProvider {
  private Stdlib stdlib;

  BasicStdlibProvider(Stdlib stdlib) {
    this.stdlib = stdlib;
  }

  @Override
  public Map<String, Import> find(Iterable<String> identifiers) {
    Map<String, Import> found = new HashMap<>();
    for (String identifier : identifiers) {
      if (!stdlib.getClasses().containsKey(identifier)) {
        continue;
      }

      if (stdlib.getClasses().get(identifier).length == 1) {
        found.put(identifier, stdlib.getClasses().get(identifier)[0]);
        continue;
      }

      throw new RuntimeException("not implemented");
    }

    return found;
  }
}
