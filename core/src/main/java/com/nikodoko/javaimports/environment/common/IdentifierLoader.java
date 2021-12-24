package com.nikodoko.javaimports.environment.common;

import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import java.util.Set;

/**
 * Loads all public and protected identifiers for a given class (identifier by its {@link Import}).
 */
@FunctionalInterface
public interface IdentifierLoader {
  Set<Identifier> loadIdentifiers(Import i);
}
