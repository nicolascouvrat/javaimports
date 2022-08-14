package com.nikodoko.javaimports.environment.maven;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A simplified representation of a Maven POM, exposing only the dependencies. It can be extended
 * with other {@code FlatPom}s to enrich it.
 */
class FlatPom {
  private List<MavenDependency> dependencies;
  private Map<MavenCoordinates.Versionless, MavenDependency> managedDependencies;
  private Properties properties;
  private Optional<MavenParent> maybeParent;

  private FlatPom(
      List<MavenDependency> dependencies,
      List<MavenDependency> managedDependencies,
      Properties properties,
      Optional<MavenParent> maybeParent) {
    this.dependencies = dependencies;
    this.managedDependencies =
        managedDependencies.stream()
            .collect(
                // If two managed dependencies have the same coordinates, the first takes precedence
                Collectors.toMap(
                    d -> d.coordinates().hideVersion(), Function.identity(), (a, b) -> a));
    this.properties = properties;
    this.maybeParent = maybeParent;
    useManagedVersionWhenNeeded();
    substitutePropertiesWhenPossible();
  }

  private void useManagedVersionWhenNeeded() {
    dependencies =
        dependencies.stream()
            .map(
                d -> {
                  var managed = managedDependencies.get(d.coordinates().hideVersion());
                  if (managed == null) {
                    return d;
                  }

                  var version = d.hasVersion() ? d.version() : managed.version();
                  var scope =
                      d.scope().isPresent() ? d.scope().get() : managed.scope().orElse(null);
                  var optional = d.optional() ? d.optional() : managed.optional();
                  var classifier = d.classifier().orElse(null);

                  // TODO: it seems that, while technically allowing it, maven does not yet support
                  // exclusions in dependencyManagement? In any case, ignore it for now
                  return new MavenDependency(
                      d.groupId(),
                      d.artifactId(),
                      version,
                      d.type(),
                      classifier,
                      scope,
                      optional,
                      d.exclusions());
                })
            .collect(Collectors.toList());
  }

  private void substitutePropertiesWhenPossible() {
    dependencies.forEach(d -> d.substitute(properties));
    managedDependencies.values().forEach(d -> d.substitute(properties));
  }

  /**
   * If the pom is already well defined, don't do anything. Otherwise, used the managed version and
   * resolve properties.
   */
  void merge(FlatPom other) {
    other.managedDependencies.forEach((k, v) -> this.managedDependencies.putIfAbsent(k, v));
    // The other properties have lower priority so we put them as defaults
    var newProperties = new Properties(other.properties);
    properties.forEach((k, v) -> newProperties.setProperty((String) k, (String) v));
    this.properties = newProperties;
    this.maybeParent = other.maybeParent;
    useManagedVersionWhenNeeded();
    substitutePropertiesWhenPossible();
  }

  List<MavenDependency> dependencies() {
    return dependencies;
  }

  Collection<MavenDependency> managedDependencies() {
    return managedDependencies.values();
  }

  static Builder builder() {
    return new Builder();
  }

  Optional<MavenParent> maybeParent() {
    return maybeParent;
  }

  boolean hasParent() {
    return maybeParent.isPresent();
  }

  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("dependencies", dependencies)
        .add("maybeParent", maybeParent)
        .toString();
  }

  static class Builder {
    private List<MavenDependency> dependencies = new ArrayList<>();
    private List<MavenDependency> managedDependencies = new ArrayList<>();
    private Properties properties = new Properties();
    private Optional<MavenParent> maybeParent = Optional.empty();

    Builder dependencies(List<MavenDependency> dependencies) {
      this.dependencies = dependencies;
      return this;
    }

    Builder managedDependencies(List<MavenDependency> managedDependencies) {
      this.managedDependencies = managedDependencies;
      return this;
    }

    Builder properties(Properties properties) {
      this.properties = properties;
      return this;
    }

    Builder maybeParent(Optional<MavenParent> maybeParent) {
      this.maybeParent = maybeParent;
      return this;
    }

    FlatPom build() {
      return new FlatPom(dependencies, managedDependencies, properties, maybeParent);
    }
  }
}
