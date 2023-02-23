package com.nikodoko.javaimports.fixer.internal;

import com.nikodoko.javaimports.Options;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.environment.Environment;
import com.nikodoko.javaimports.environment.Environments;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.stdlib.StdlibProvider;
import com.nikodoko.javaimports.stdlib.StdlibProviders;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    this.result.orphans = file.orphans();
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
    resolveAllJavaLang();
    resolveUsingImports();
    resolveUsingSiblings();
  }

  private void resolveAllJavaLang() {
    Set<Identifier> inJavaLang = new HashSet<>();
    for (var unresolved : result.unresolved) {
      if (stdlib.isInJavaLang(unresolved.toString())) {
        inJavaLang.add(unresolved);
      }
    }

    result.unresolved = difference(result.unresolved, inJavaLang);
  }

  // FIXME: this does not do anything about the situation where we import a class that we extend
  // in the file. The problem is that we would need informations on the methods provided by said
  // class so that we can decide which identifiers are still unresoled.
  // We should probably have shortcut here that directly goes to find that package? But we most
  // likely need environment information for this...
  private void resolveUsingImports() {
    result.unresolved = difference(result.unresolved, file.importedIdentifiers());
    result.orphans =
        result.orphans.stream()
            .map(o -> o.addDeclarations(file.importedIdentifiers()))
            .collect(Collectors.toSet());
  }

  private void resolveUsingSiblings() {
    for (ParsedFile sibling : siblings) {
      resolveUsingSibling(sibling);
    }
  }

  private void resolveUsingSibling(ParsedFile sibling) {
    result.unresolved = difference(result.unresolved, sibling.topLevelDeclarations());
    result.orphans =
        result.orphans.stream()
            .map(o -> o.addDeclarations(sibling.topLevelDeclarations()))
            .collect(Collectors.toSet());
  }

  private <T> Set<T> difference(Set<T> original, Set<T> toRemove) {
    Set<T> result = new HashSet<>();
    for (var identifier : original) {
      if (!toRemove.contains(identifier)) {
        result.add(identifier);
      }
    }

    return result;
  }
}
