package com.nikodoko.javaimports.environment.shared;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@FunctionalInterface
public interface SourceFiles {
  List<Path> get() throws IOException;
}
