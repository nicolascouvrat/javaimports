package com.nikodoko.javaimports.stdlib.internal;

import com.nikodoko.javaimports.parser.Import;

public interface Stdlib {
  public Import[] getClassesFor(String identifier);
}
