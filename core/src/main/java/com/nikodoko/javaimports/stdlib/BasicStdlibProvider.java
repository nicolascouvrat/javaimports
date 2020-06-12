package com.nikodoko.javaimports.stdlib;

import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.stdlib.internal.Stdlib;
import java.util.HashMap;
import java.util.Map;

public class BasicStdlibProvider implements StdlibProvider {
  private Stdlib stdlib;

  BasicStdlibProvider(Stdlib stlib) {
    this.stdlib = stdlib;
  }

  @Override
  public Map<String, Import> find(Iterable<String> identifiers) {
    return new HashMap<>();
  }
}
