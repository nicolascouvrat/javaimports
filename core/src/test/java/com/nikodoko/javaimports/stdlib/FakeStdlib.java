package com.nikodoko.javaimports.stdlib;

import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.stdlib.internal.Stdlib;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FakeStdlib implements Stdlib {
  private final Map<Identifier, List<Import>> classes;

  public FakeStdlib(Import... imports) {
    this.classes =
        Arrays.stream(imports).collect(Collectors.groupingBy(i -> i.selector.identifier()));
  }

  public Import[] getClassesFor(Identifier identifier) {
    return Optional.ofNullable(classes.get(identifier))
        .map(c -> c.stream().toArray(Import[]::new))
        .orElse(null);
  }
}
