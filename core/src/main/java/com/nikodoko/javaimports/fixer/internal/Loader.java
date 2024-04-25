package com.nikodoko.javaimports.fixer.internal;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.environment.Environment;
import com.nikodoko.javaimports.environment.Environments;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.stdlib.StdlibProvider;
import com.nikodoko.javaimports.stdlib.StdlibProviders;
import java.util.HashSet;
import java.util.Set;

/**
 * Uses additional information, such as files from the same package, to determine which identifiers
 * are truly unresolved, and which classes are truly not extendable.
 */
public class Loader {
  private Set<ParsedFile> siblings = new HashSet<>();
  private StdlibProvider stdlib = StdlibProviders.empty();
  private Environment environment = Environments.empty();
  private final ParsedFile file;

  private Loader(ParsedFile file) {
    this.file = file;
  }

  /** Create a {@code Loader} for the given {@code file}. */
  public static Loader of(ParsedFile file) {
    return new Loader(file);
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
    // in other folders of the same project
    this.siblings = environment.filesInPackage(file.packageName());
  }

  /**
   * Try to find which identifiers are still unresolved, using various information in addition to
   * the file itself.
   */
  public void load() {
    resolveJavaLangObject();
    resolveAllJavaLang();
    resolveUsingImports();
    resolveUsingSiblings();
  }

  /**
   * We could have all {@code ClassEntity} emitted by the parser extend java.lang.Object and rely on
   * the extends machinery and the {@code ClassLibrary}, but this allows us to short-circuit that
   * and finish faster.
   */
  private void resolveJavaLangObject() {
    Set<Identifier> inJavaLangObject = new HashSet<>();
    for (var unresolved : file.unresolved()) {
      if (ClassEntity.JAVA_LANG_OBJECT.declarations.contains(unresolved)) {
        inJavaLangObject.add(unresolved);
      }
    }

    file.addDeclarations(inJavaLangObject);
  }

  private void resolveAllJavaLang() {
    Set<Identifier> inJavaLang = new HashSet<>();
    for (var unresolved : file.unresolved()) {
      if (stdlib.isInJavaLang(unresolved)) {
        inJavaLang.add(unresolved);
      }
    }

    file.addDeclarations(inJavaLang);
  }

  // FIXME: this does not do anything about the situation where we import a class that we extend
  // in the file. The problem is that we would need informations on the methods provided by said
  // class so that we can decide which identifiers are still unresoled.
  // We should probably have shortcut here that directly goes to find that package? But we most
  // likely need environment information for this...
  private void resolveUsingImports() {
    file.addDeclarations(file.importedIdentifiers());
  }

  private void resolveUsingSiblings() {
    for (ParsedFile sibling : siblings) {
      resolveUsingSibling(sibling);
    }
  }

  private void resolveUsingSibling(ParsedFile sibling) {
    file.addDeclarations(sibling.topLevelDeclarations());
  }
}
