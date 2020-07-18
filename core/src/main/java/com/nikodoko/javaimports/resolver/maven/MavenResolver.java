package com.nikodoko.javaimports.resolver.maven;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.Sets;
import com.nikodoko.javaimports.ImporterException;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;
import com.nikodoko.javaimports.resolver.ImportWithDistance;
import com.nikodoko.javaimports.resolver.PackageDistance;
import com.nikodoko.javaimports.resolver.Resolver;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
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
import java.util.stream.Collectors;

public class MavenResolver implements Resolver {
  private static Logger log = Logger.getLogger(Parser.class.getName());

  private final Path root;
  private final Path fileBeingResolved;
  private final Options options;
  private Map<String, List<ImportWithDistance>> importsByIdentifier = new HashMap<>();
  private List<JavaFile> filesInProject = new ArrayList<>();
  private boolean isInitialized = false;
  private final PackageDistance distance;

  private final MavenProjectScanner scanner;

  public MavenResolver(
      Path root, Path fileBeingResolved, String pkgBeingResolved, Options options) {
    this.root = root;
    this.fileBeingResolved = fileBeingResolved;
    this.options = options;
    this.scanner = MavenProjectScanner.withRoot(root).excluding(fileBeingResolved);
    this.distance = PackageDistance.from(pkgBeingResolved);
  }

  @Override
  public Set<ParsedFile> filesInPackage(String packageName) {
    MavenProjectScanner.Result scan = scanner.scanFilesInPackage(packageName);
    if (options.debug()) {
      log.info(String.format("scanned files in package %s: %s", packageName, scan));
    }

    return Sets.newHashSet(scan.files);
  }

  @Override
  public Optional<Import> find(String identifier) {
    if (!isInitialized) {
      safeInit();
    }

    List<ImportWithDistance> imports = importsByIdentifier.get(identifier);
    if (imports == null) {
      return Optional.empty();
    }

    return Optional.of(Collections.min(imports).i);
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
    for (Path javaFile : javaFilesNotBeingResolved()) {
      filesInProject.add(parseFile(javaFile));
    }

    for (JavaFile file : filesInProject) {
      for (ImportWithDistance i : extractImports(file.contents)) {
        List<ImportWithDistance> importsForIdentifier =
            importsByIdentifier.getOrDefault(i.i.name(), new ArrayList<>());
        importsForIdentifier.add(i);
        importsByIdentifier.put(i.i.name(), importsForIdentifier);
      }
    }

    for (ImportWithDistance i : extractImportsInDependencies()) {
      List<ImportWithDistance> importsForIdentifier =
          importsByIdentifier.getOrDefault(i.i.name(), new ArrayList<>());
      importsForIdentifier.add(i);
      importsByIdentifier.put(i.i.name(), importsForIdentifier);
    }

    isInitialized = true;
  }

  private List<ImportWithDistance> extractImportsInDependencies() throws IOException {
    List<MavenDependency> dependencies = findAllDependencies();
    if (options.debug()) {
      log.info(String.format("found %d dependencies: %s", dependencies.size(), dependencies));
    }

    List<ImportWithDistance> imports = new ArrayList<>();
    for (MavenDependency dependency : dependencies) {
      imports.addAll(resolveDependency(dependency));
    }

    return imports;
  }

  private List<ImportWithDistance> resolveDependency(MavenDependency dependency) {
    Path repository = Paths.get(System.getProperty("user.home"), ".m2/repository");
    MavenDependencyResolver resolver = new MavenDependencyResolver(fileBeingResolved, repository);
    try {
      Path location = resolver.resolve(dependency);
      if (options.debug()) {
        log.info(String.format("looking for dependency %s at %s", dependency, location));
      }

      return resolver.load(location);
    } catch (Exception e) {
      if (options.debug()) {
        log.log(Level.WARNING, String.format("could not resolve dependency %s", dependency), e);
      }

      return new ArrayList<>();
    }
  }

  private List<MavenDependency> findAllDependencies() throws IOException {
    MavenDependencyFinder finder = new MavenDependencyFinder();
    scanPom(root, finder);

    Path target = root.getParent();
    while (target != null && !finder.allFound()) {
      if (!hasPom(target)) {
        break;
      }

      scanPom(target, finder);
      target = target.getParent();
    }

    return finder.result();
  }

  private boolean hasPom(Path directory) {
    Path pom = Paths.get(directory.toString(), "pom.xml");
    return Files.exists(pom);
  }

  private void scanPom(Path directory, MavenDependencyFinder finder) throws IOException {
    Path pom = Paths.get(directory.toString(), "pom.xml");
    try (FileReader reader = new FileReader(pom.toFile())) {
      finder.scan(reader);
    }
  }

  private List<Path> javaFilesNotBeingResolved() throws IOException {
    return Files.find(
            root,
            100,
            (path, attributes) ->
                path.toString().endsWith(".java") && !path.equals(fileBeingResolved))
        .collect(Collectors.toList());
  }

  private List<ImportWithDistance> extractImports(ParsedFile file) {
    List<ImportWithDistance> imports = new ArrayList<>();
    for (String identifier : file.topLevelDeclarations()) {
      Import i = new Import(identifier, file.packageName(), false);
      imports.add(new ImportWithDistance(i, distance.to(file.packageName())));
    }

    return imports;
  }

  private JavaFile parseFile(Path path) throws IOException, ImporterException {
    String source = new String(Files.readAllBytes(path), UTF_8);
    JavaFile file = new JavaFile();
    file.contents = new Parser(Options.defaults()).parse(path, source);
    file.path = path;
    return file;
  }
}
