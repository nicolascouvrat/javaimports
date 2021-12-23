package com.nikodoko.javaimports.environment.maven;

import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;

public class MavenPomLoader {
  // If <parent></parent> is present but no <relativePath> is specified then maven will default to
  // this relative path
  private static final Path DEFAULT_PARENT = Paths.get("../pom.xml");
  private static final String DEFAULT_SCOPE = "compile";
  private static final String DEFAULT_TYPE = "jar";

  static final class Result {
    final FlatPom pom;
    final List<MavenEnvironmentException> errors;

    private Result(FlatPom pom, List<MavenEnvironmentException> errors) {
      this.pom = pom;
      this.errors = errors;
    }

    static Result error(MavenEnvironmentException error) {
      return new Result(FlatPom.builder().build(), List.of(error));
    }

    static Result complete(FlatPom pom) {
      return new Result(pom, List.of());
    }

    public String toString() {
      return MoreObjects.toStringHelper(this).add("pom", pom).add("errors", errors).toString();
    }
  }

  static Result load(Path pom) {
    return tryToScan(pom);
  }

  private static Result tryToScan(Path pom) {
    try {
      return scan(pom);
    } catch (IOException e) {
      return Result.error(
          new MavenEnvironmentException(String.format("could not scan pom %s", pom), e));
    }
  }

  private static Result scan(Path pom) throws IOException {
    var model = new DefaultModelReader().read(pom.toFile(), null);
    var dependencies = convert(model.getDependencies());
    var managedDependencies =
        convert(
            Optional.ofNullable(model.getDependencyManagement())
                .map(DependencyManagement::getDependencies)
                .orElse(List.of()));
    var properties = model.getProperties();

    return Result.complete(
        FlatPom.builder()
            .dependencies(dependencies)
            .managedDependencies(managedDependencies)
            .maybeParent(getMaybeParent(model))
            .properties(properties)
            .build());
  }

  private static Optional<Path> getMaybeParent(Model model) {
    if (model.getParent() == null) {
      return Optional.empty();
    }

    if (model.getParent().getRelativePath() == null) {
      return Optional.of(DEFAULT_PARENT);
    }

    // If a relative path is explicitely set to empty, it means maven won't look for a local parent
    // pom. For our purposes, this is as if this POM has no parent
    if (model.getParent().getRelativePath().equals("")) {
      return Optional.empty();
    }

    return Optional.of(Paths.get(model.getParent().getRelativePath()));
  }

  private static List<MavenDependency> convert(List<Dependency> dependencies) {
    return dependencies.stream()
        .map(
            d ->
                new MavenDependency(
                    d.getGroupId(),
                    d.getArtifactId(),
                    d.getVersion(),
                    Optional.ofNullable(d.getType()).orElse(DEFAULT_TYPE),
                    Optional.ofNullable(d.getScope()).orElse(DEFAULT_SCOPE),
                    d.isOptional()))
        .collect(Collectors.toList());
  }
}
