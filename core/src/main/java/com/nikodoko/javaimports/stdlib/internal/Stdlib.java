package com.nikodoko.javaimports.stdlib.internal;

import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import java.util.Arrays;

public interface Stdlib {
  public Import[] getClassesFor(Identifier identifier);

  static Import newImport(String dotSelector, boolean isStatic) {
    var identifiers = dotSelector.split("\\.");
    var selector = Selector.of(Arrays.asList(identifiers));
    return new Import(selector, isStatic);
  }
}
