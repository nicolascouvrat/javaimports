package com.nikodoko.javaimports.stdlib;

import com.nikodoko.javaimports.parser.Import;
import java.util.Map;

public interface StdlibProvider {
  public Map<String, Import> find(Iterable<String> identifiers);
}
