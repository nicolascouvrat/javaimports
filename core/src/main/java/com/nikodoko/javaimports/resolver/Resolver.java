package com.nikodoko.javaimports.resolver;

import com.nikodoko.javaimports.parser.Import;
import java.util.Optional;

public interface Resolver {
  Optional<Import> find(String identifier);
}
