package com.nikodoko.javaimports.resolver.maven;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.nikodoko.javaimports.ImporterException;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;
import com.nikodoko.javaimports.resolver.JavaProject;
import com.nikodoko.javaimports.resolver.PackageDistance;
import com.nikodoko.javaimports.resolver.Resolver;
import java.io.IOException;
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

public class MavenResolver implements Resolver {
  private static Logger log = Logger.getLogger(Parser.class.getName());

  private final Path root;
  private final Path fileBeingResolved;
  private final Options options;
  private Map<String, Import> bestAvailableImports = new HashMap<>();
  private boolean isInitialized = false;
  private final PackageDistance distance;

  // XXX
  private static final Path DEFAULT_REPOSITORY =
      Paths.get(System.getProperty("user.home"), ".m2/repository");
  private JavaProject project;
  private boolean projectIsParsed = false;
  // TODO: maybe don't save this
  private final MavenDependencyFinder dependencyFinder;
  private final Path repository;

  public MavenResolver(
      Path root, Path fileBeingResolved, String pkgBeingResolved, Options options) {
    this.root = root;
    this.fileBeingResolved = fileBeingResolved;
    this.options = options;
    this.distance = PackageDistance.from(pkgBeingResolved);
    this.dependencyFinder = new MavenDependencyFinder();
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
      safeInit();
    }

    return Optional.ofNullable(bestAvailableImports.get(identifier));
  }

  private void safeInit() {
    try {
      init();
    } catch (Exception e) {
      // TODO: decide what to do here
      e.printStackTrace();
    }
  }

  private void init() throws IOException, ImporterException {
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

  private List<Import> extractImportsInDependencies() throws IOException {
    List<MavenDependency> dependencies = dependencyFinder.findAll(root).dependencies;
    if (options.debug()) {
      log.info(String.format("found %d dependencies: %s", dependencies.size(), dependencies));
    }

    List<Import> imports = new ArrayList<>();
    for (MavenDependency dependency : dependencies) {
      imports.addAll(resolveDependency(dependency));
    }

    return imports;
  }

  private List<Import> resolveDependency(MavenDependency dependency) {
    MavenDependencyResolver resolver = MavenDependencyResolver.withRepository(repository);
    MavenDependencyLoader loader = new MavenDependencyLoader();
    try {
      Path location = resolver.resolve(dependency);
      if (options.debug()) {
        log.info(String.format("looking for dependency %s at %s", dependency, location));
      }

      return loader.load(location);
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
