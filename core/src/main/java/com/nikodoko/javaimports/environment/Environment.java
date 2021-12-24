package com.nikodoko.javaimports.environment;

import com.nikodoko.javaimports.common.ClassProvider;
import com.nikodoko.javaimports.common.ImportProvider;
import com.nikodoko.javaimports.parser.ParsedFile;
import java.util.Set;

/**
 * A build system-agnostic representation of a Java project's environment, that can be queried to
 * get information about importable symbols.
 */
public interface Environment extends ImportProvider, ClassProvider {
  /**
   * Returns all files in the package of a given {@code packageName}.
   *
   * <p>This uses the actual {@code package} declaration at the top of Java files, and will
   * therefore properly detect files of the same package in different directories.
   */
  Set<ParsedFile> filesInPackage(String packageName);
}
