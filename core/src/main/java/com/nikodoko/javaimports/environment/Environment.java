package com.nikodoko.javaimports.environment;

import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import java.util.Optional;
import java.util.Set;

public interface Environment {
  Optional<Import> search(String identifier);

  Set<ParsedFile> filesInPackage(String packageName);
}
