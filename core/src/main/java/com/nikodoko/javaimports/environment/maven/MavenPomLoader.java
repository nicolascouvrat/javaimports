package com.nikodoko.javaimports.environment.maven;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.common.telemetry.Tag;
import com.nikodoko.javaimports.common.telemetry.Traces;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.DefaultModelReader;

public class MavenPomLoader {
  // If <parent></parent> is present but no <relativePath> is specified then maven will default to
  // this relative path
  private static final Path DEFAULT_PARENT_PATH = Paths.get("../pom.xml");
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
    var span = Traces.createSpan("MavenPomLoader.load", new Tag("pom_path", pom));
    try (var __ = Traces.activate(span)) {
      return tryToScan(pom);
    } finally {
      span.finish();
    }
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
    enrichProperties(properties, model, pom);

    return Result.complete(
        FlatPom.builder()
            .dependencies(dependencies)
            .managedDependencies(managedDependencies)
            .maybeParent(getMaybeParent(model))
            .properties(properties)
            .build());
  }

  private static void enrichProperties(Properties props, Model model, Path pom) {
    // GroupId, Version and ArtifactId should never be null, see
    // https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#minimal-pom
    // However, child poms do not have it and use the parent's instead
    if (model.getGroupId() != null) {
      props.setProperty("project.groupId", model.getGroupId());
    }
    if (model.getVersion() != null) {
      props.setProperty("project.version", model.getVersion());
    }
  }

  private static Optional<MavenParent> getMaybeParent(Model model) {
    if (model.getParent() == null) {
      return Optional.empty();
    }

    var parent = model.getParent();
    var coordinates =
        new MavenCoordinates(
            parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), "pom");
    return Optional.of(new MavenParent(coordinates, getMaybeParentRelativePath(parent)));
  }

  private static Optional<Path> getMaybeParentRelativePath(Parent parent) {
    if (parent.getRelativePath() == null) {
      return Optional.of(DEFAULT_PARENT_PATH);
    }

    if (parent.getRelativePath().equals("")) {
      return Optional.empty();
    }

    return Optional.of(Paths.get(parent.getRelativePath()));
  }

  private static List<MavenDependency> convert(List<Dependency> dependencies) {
    return dependencies.stream()
        .map(
            d ->
                new MavenDependency(
                    d.getGroupId(),
                    d.getArtifactId(),
                    d.getVersion(),
                    d.getType(),
                    d.getScope(),
                    d.isOptional()))
        .collect(Collectors.toList());
  }
}
