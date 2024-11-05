package com.nikodoko.javaimports.environment.bazel;

import com.nikodoko.javaimports.environment.shared.Dependency;
import java.nio.file.Path;

public record BazelDependency(Dependency.Kind kind, Path path) implements Dependency {}
