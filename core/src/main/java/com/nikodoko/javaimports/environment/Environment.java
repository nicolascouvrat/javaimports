package com.nikodoko.javaimports.environment;

import com.nikodoko.javaimports.common.ClassProvider;
import com.nikodoko.javaimports.common.ImportProvider;
import com.nikodoko.javaimports.common.JavaSourceFile;
import java.util.List;

/**
 * A build system-agnostic representation of a Java project's environment, that can be queried to
 * get information about importable symbols.
 */
public interface Environment extends ImportProvider, ClassProvider {
  List<JavaSourceFile> siblings();

  boolean increasePrecision();
}
