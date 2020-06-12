package com.nikodoko.javaimports.stdlib.internal;

import com.nikodoko.javaimports.parser.Import;
import java.util.Map;

public interface Stdlib {
  public Map<String, Import[]> getClasses();
}
