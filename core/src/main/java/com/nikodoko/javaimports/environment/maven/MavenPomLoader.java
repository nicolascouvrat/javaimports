package com.nikodoko.javaimports.environment.maven;

import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.io.DefaultModelReader;

public class MavenPomLoader {
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

  Result load(Path pom) {
    return tryToScan(pom);
  }

  private Result tryToScan(Path pom) {
    try {
      return scan(pom);
    } catch (IOException e) {
      return Result.error(
          new MavenEnvironmentException(String.format("could not scan pom %s", pom), e));
    }
  }

  private Result scan(Path pom) throws IOException {
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
            .properties(properties)
            .build());
  }

  private List<MavenDependency> convert(List<Dependency> dependencies) {
    return dependencies.stream()
        .map(d -> new MavenDependency(d.getGroupId(), d.getArtifactId(), d.getVersion()))
        .collect(Collectors.toList());
  }
}
