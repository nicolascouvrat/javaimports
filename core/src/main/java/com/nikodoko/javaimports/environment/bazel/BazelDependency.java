package com.nikodoko.javaimports.environment.bazel;

import com.nikodoko.javaimports.environment.shared.Dependency;
import java.nio.file.Path;

public record BazelDependency(Dependency.Kind kind, Path path) implements Dependency {
  BazelDependency(int rank, Path path) {
    this(getKind(rank), path);
  }

  private static Dependency.Kind getKind(int rank) {
    // Obviously, rank 1 dependencies are direct, but we also consider rank 2 as direct. That's
    // because if you include a target that's a `java_library`, that target will be rank 1, but its
    // source file themselves will be rank 2. Similarly, the @maven:group_id_artifact_id will be
    // rank 1 but the actual `.jar` it points to will be rank 2.
    if (rank <= 2) {
      return Dependency.Kind.DIRECT;
    }

    return Dependency.Kind.TRANSITIVE;
  }
}
