package com.nikodoko.packagetest;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Contains the result of {@link com.nikodoko.packagetest.Export#of}. */
public class Exported {
  // Denotes that cleanup has been already done
  private static final Path EMPTY = Paths.get("");

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
  void markAsWritten(String module, String fragment, Path path) {
    checkNotNull(path, "why mark a null path as written?");
    Map<String, Path> moduleFiles = written.get(module);
    if (moduleFiles == null) {
      moduleFiles = new HashMap<>();
      written.put(module, moduleFiles);
    }

    moduleFiles.put(fragment, path);
  }

  /**
   * Removes the directory at the root of this {@code Exported} and all its contents.
   *
   * <p>This is safe to call multiple times.
   *
   * @throws IOException if an I/O error occurs
   */
  public void cleanup() throws IOException {
    if (root == EMPTY) {
      return;
    }

    Files.walk(root).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);

    root = EMPTY;
  }
}
