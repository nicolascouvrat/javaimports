package com.nikodoko.javaimports.parser.internal;

import java.util.Optional;

public interface ClassSelector {
  public String selector();

  public Optional<ClassSelector> next();
}
