package com.nikodoko.javaimports.fixer.internal;

import com.google.common.collect.ImmutableSet;
import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.environment.Environment;
import com.nikodoko.javaimports.environment.Environments;
import com.nikodoko.javaimports.parser.ClassExtender;
import com.nikodoko.javaimports.parser.ClassHierarchies;
import com.nikodoko.javaimports.parser.ClassHierarchy;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.stdlib.StdlibProvider;
import com.nikodoko.javaimports.stdlib.StdlibProviders;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Uses additional information, such as files from the same package, to determine which identifiers
 * are truly unresolved, and which classes are truly not extendable.
 */
public class Loader {
  private static Logger log = Logger.getLogger(Loader.class.getName());

  private Set<ParsedFile> siblings = new HashSet<>();
  private StdlibProvider stdlib = StdlibProviders.empty();
  private Environment environment = Environments.empty();
  private ParsedFile file;
  private Options options;

  private LoadResult result = new LoadResult();

  private Loader(ParsedFile file, Options options) {
    this.file = file;
    this.result.unresolved = file.notYetResolved();
    this.result.orphans = file.notFullyExtendedClasses();
    this.options = options;
  }

  /** Create a {@code Loader} for the given {@code file}. */
  public static Loader of(ParsedFile file, Options options) {
    return new Loader(file, options);
  }

  /** Add sibling files to the loader */
  public void addSiblings(Set<ParsedFile> siblings) {
    this.siblings = siblings;
  }

  public void addStdlibProvider(StdlibProvider provider) {
    this.stdlib = provider;
  }

  public void addEnvironment(Environment environment) {
    this.environment = environment;
    // The environment lets us find not only the siblings in the same folder, but also the siblings
    // in
    // other folders of the same project
    this.siblings = environment.filesInPackage(file.packageName());
  }

  /** Returns the result of this loader */
  public LoadResult result() {
    return result;
  }

  /**
   * Try to find which identifiers are still unresolved, using various information in addition to
   * the file itself.
   */
  public void load() {
    extendAllClasses();
    resolveAllJavaLang();
    resolveUsingImports();
    resolveUsingSiblings();

    addSiblingImportsAsCandidates();
    addStdlibCandidates();
    addExternalCandidates();
  }

  private void resolveAllJavaLang() {
    Set<String> inJavaLang = new HashSet<>();
    for (String unresolved : result.unresolved) {
      if (stdlib.isInJavaLang(unresolved)) {
        inJavaLang.add(unresolved);
      }
    }

    result.unresolved = difference(result.unresolved, inJavaLang);
  }

  private void addExternalCandidates() {
    allStillUnresolved().stream()
        .map(environment::search)
        .filter(Optional::isPresent)
        .forEach(candidate -> result.candidates.add(Candidates.Priority.EXTERNAL, candidate.get()));
  }

  private void addStdlibCandidates() {
    Map<String, Import> stdlibCandidates = stdlib.find(allStillUnresolved());
    result.candidates.add(Candidates.Priority.STDLIB, stdlibCandidates.values());
  }

  private void addSiblingImportsAsCandidates() {
    for (ParsedFile sibling : siblings) {
      result.candidates.add(Candidates.Priority.SIBLING, sibling.imports().values());
    }
  }

  private Set<String> allStillUnresolved() {
    ImmutableSet.Builder builder = ImmutableSet.builder().addAll(result.unresolved);
    for (ClassExtender orphan : result.orphans) {
      builder.addAll(orphan.notYetResolved());
    }

    Set<String> all = builder.build();
    return all;
  }

  // FIXME: this does not do anything about the situation where we import a class that we extend
  // in the file. The problem is that we would need informations on the methods provided by said
  // class so that we can decide which identifiers are still unresoled.
  // We should probably have shortcut here that directly goes to find that package? But we most
  // likely need environment information for this...
  private void resolveUsingImports() {
    result.unresolved = difference(result.unresolved, file.imports().keySet());

    for (ClassExtender e : result.orphans) {
      e.resolveUsing(file.imports().keySet());
    }
  }

  private void resolveUsingSiblings() {
    for (ParsedFile sibling : siblings) {
      resolveUsingSibling(sibling);
    }
  }

  private void resolveUsingSibling(ParsedFile sibling) {
    result.unresolved = difference(result.unresolved, sibling.topLevelDeclarations());

    for (ClassExtender e : result.orphans) {
      e.resolveUsing(sibling.topLevelDeclarations());
    }
  }

  private Set<String> difference(Set<String> original, Set<String> toRemove) {
    Set<String> result = new HashSet<>();
    for (String identifier : original) {
      if (!toRemove.contains(identifier)) {
        result.add(identifier);
      }
    }

    return result;
  }

  private void extendAllClasses() {
    Set<ClassExtender> notFullyExtendedClasses = new HashSet<>();
    for (ClassExtender e : result.orphans) {
      extendUsingSiblings(e);
      if (e.isFullyExtended()) {
        result.unresolved.addAll(e.notYetResolved());
        continue;
      }

      notFullyExtendedClasses.add(e);
    }

    result.orphans = notFullyExtendedClasses;
  }

  private void extendUsingSiblings(ClassExtender toExtend) {
    ClassHierarchy[] hierarchies = new ClassHierarchy[siblings.size() + 1];
    // Some siblings' classes might depend on classes defined in the file to fix
    hierarchies[0] = file.classHierarchy();
    int i = 1;
    for (ParsedFile sibling : siblings) {
      hierarchies[i++] = sibling.classHierarchy();
    }

    ClassHierarchy combined = ClassHierarchies.combine(hierarchies);
    if (options.debug()) {
      log.info(String.format("extending %s with %s", toExtend, combined));
    }

    toExtend.extendAsMuchAsPossibleUsing(combined);
  }
}
