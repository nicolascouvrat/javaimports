package com.nikodoko.javaimports.environment.maven;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;
import com.nikodoko.javaimports.environment.JavaProject;
import com.nikodoko.javaimports.environment.PackageDistance;
import com.nikodoko.javaimports.environment.Resolver;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MavenEnvironment implements Resolver {
  private static Logger log = Logger.getLogger(Parser.class.getName());
  private static final Path DEFAULT_REPOSITORY =
      Paths.get(System.getProperty("user.home"), ".m2/repository");

  private final Path root;
  private final Path fileBeingResolved;
  private final Options options;
  private final PackageDistance distance;
  private final Path repository;

  private Map<String, Import> bestAvailableImports = new HashMap<>();
  private JavaProject project;
  private boolean projectIsParsed = false;
  private boolean isInitialized = false;

  public MavenEnvironment(
      Path root, Path fileBeingResolved, String pkgBeingResolved, Options options) {
    this.root = root;
    this.fileBeingResolved = fileBeingResolved;
    this.options = options;
    this.distance = PackageDistance.from(pkgBeingResolved);
    this.repository =
        options.repository().isPresent() ? options.repository().get() : DEFAULT_REPOSITORY;
  }

  @Override
  public Set<ParsedFile> filesInPackage(String packageName) {
    parseProjectIfNeeded();
    return Sets.newHashSet(project.filesInPackage(packageName));
  }

  @Override
  public Optional<Import> find(String identifier) {
    if (!isInitialized) {
      init();
    }

    return Optional.ofNullable(bestAvailableImports.get(identifier));
  }

  private void init() {
    parseProjectIfNeeded();

    List<Import> imports = extractImportsInDependencies();
    for (ParsedFile file : project.allFiles()) {
      imports.addAll(extractImports(file));
    }

    Collections.sort(imports, (a, b) -> distance.to(a.qualifier()) - distance.to(b.qualifier()));
    for (Import i : imports) {
      if (bestAvailableImports.containsKey(i.name())) {
        continue;
      }

      bestAvailableImports.put(i.name(), i);
    }

    isInitialized = true;
  }

  private void parseProjectIfNeeded() {
    if (projectIsParsed) {
      return;
    }

    MavenProjectParser parser = MavenProjectParser.withRoot(root).excluding(fileBeingResolved);
    MavenProjectParser.Result parsed = parser.parseAll();
    if (options.debug()) {
      log.info(
          String.format(
              "parsed project (total of %d files): %s",
              Iterables.size(parsed.project.allFiles()), parsed));
    }

    project = parsed.project;
    projectIsParsed = true;
  }

  private List<Import> extractImportsInDependencies() {
    MavenDependencyFinder.Result found = new MavenDependencyFinder().findAll(root);
    if (options.debug()) {
      log.info(String.format("found %d dependencies: %s", found.dependencies.size(), found));
    }

    List<Import> imports = new ArrayList<>();
    for (MavenDependency dependency : found.dependencies) {
      imports.addAll(resolveAndLoad(dependency));
    }

    return imports;
  }

  private List<Import> resolveAndLoad(MavenDependency dependency) {
    MavenDependencyResolver resolver = MavenDependencyResolver.withRepository(repository);
    try {
      Path location = resolver.resolve(dependency);
      if (options.debug()) {
        log.info(String.format("looking for dependency %s at %s", dependency, location));
      }

      return new MavenDependencyLoader().load(location);
    } catch (Exception e) {
      // No matter what happens, we don't want to fail the whole importing process just for that.
      if (options.debug()) {
        log.log(Level.WARNING, String.format("could not resolve dependency %s", dependency), e);
      }

      return new ArrayList<>();
    }
  }

  private List<Import> extractImports(ParsedFile file) {
    List<Import> imports = new ArrayList<>();
    for (String identifier : file.topLevelDeclarations()) {
      imports.add(new Import(identifier, file.packageName(), false));
    }

    return imports;
  }
}
