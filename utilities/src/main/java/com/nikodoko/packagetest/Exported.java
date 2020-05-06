package com.nikodoko.packagetest;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Contains the result of {@link com.nikodoko.packagetest.Export#of}. */
public class Exported {
  private Path root;
  private Map<String, Map<String, Path>> written = new HashMap<>();

  /**
   * An {@code Exported} constructor.
   *
   * @param root its root directory
   */
  Exported(Path root) {
    this.root = root;
  }

  /** Returns the directory at the root of this {@code Exported} data. */
  public Path root() {
    return root;
  }

  /**
   * Returns an optional containing the path for a given module and fragment.
   *
   * @param module a module name
   * @param fragment a path fragment
   */
  public Optional<Path> file(String module, String fragment) {
    Map<String, Path> moduleFiles = written.get(module);
    if (moduleFiles == null) {
      return Optional.empty();
    }

    return Optional.ofNullable(moduleFiles.get(fragment));
  }

  /**
   * Signal that a file has been written at the given {@code path}.
   *
   * @param module a module name
   * @param fragment a path fragment
   * @param path the path at which the file has been written
   */
  public void markAsWritten(String module, String fragment, Path path) {
    checkNotNull(path, "why mark a null path as written?");
    Map<String, Path> moduleFiles = written.get(module);
    if (moduleFiles == null) {
      moduleFiles = new HashMap<>();
      written.put(module, moduleFiles);
    }

    moduleFiles.put(fragment, path);
  }
}
