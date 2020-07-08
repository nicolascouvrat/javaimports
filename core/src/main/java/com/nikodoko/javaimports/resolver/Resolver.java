package com.nikodoko.javaimports.resolver;

import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import java.util.Optional;
import java.util.Set;

public interface Resolver {
  Optional<Import> find(String identifier);

  Set<ParsedFile> filesInPackage(String packageName);
}
