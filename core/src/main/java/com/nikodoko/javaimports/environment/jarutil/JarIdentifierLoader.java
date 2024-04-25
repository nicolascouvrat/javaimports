package com.nikodoko.javaimports.environment.jarutil;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads all public and protected identifiers for a given class (identifier by its {@link Import}).
 */
public class JarIdentifierLoader implements JarLoader {
  ClassLoader cl = null;
  final URL[] jarUrls;

  public JarIdentifierLoader(Path jarPath) {
    this(List.of(jarPath));
  }

  public JarIdentifierLoader(Collection<Path> jarPaths) {
    this.jarUrls = jarPaths.stream().map(this::asUrl).toArray(URL[]::new);
  }

  private URL asUrl(Path path) {
    try {
      return new URL("jar:file:" + path + "!/");
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("could not form an URL for jar path: " + path, e);
    }
  }

  @Override
  public Optional<ClassEntity> loadClass(Import i) {
    var c = loadClassInternal(i);
    var identifiers = new HashSet<Identifier>();
    while (c != null) {
      identifiers.addAll(getUsableDeclaredIdentifiers(c));
      c = c.getSuperclass();
    }

    return Optional.of(ClassEntity.named(i.selector).declaring(identifiers).build());
  }

  private record IdentifierAndModifier(Identifier identifier, int modifiers) {
    static IdentifierAndModifier fromMember(Member m) {
      return new IdentifierAndModifier(new Identifier(m.getName()), m.getModifiers());
    }

    static IdentifierAndModifier fromClass(Class c) {
      return new IdentifierAndModifier(new Identifier(c.getSimpleName()), c.getModifiers());
    }
  }

  private static Set<Identifier> getUsableDeclaredIdentifiers(Class c) {
    var fields = Arrays.stream(c.getDeclaredFields()).map(IdentifierAndModifier::fromMember);
    var methods = Arrays.stream(c.getDeclaredMethods()).map(IdentifierAndModifier::fromMember);
    var classes = Arrays.stream(c.getDeclaredClasses()).map(IdentifierAndModifier::fromClass);

    return Stream.of(fields, methods, classes)
        .flatMap(Function.identity())
        .filter(JarIdentifierLoader::isUsableIdentifier)
        .map(IdentifierAndModifier::identifier)
        .collect(Collectors.toSet());
  }

  private static boolean isUsableIdentifier(IdentifierAndModifier im) {
    var modifiers = im.modifiers();
    return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
  }

  private Class loadClassInternal(Import i) {
    if (cl == null) {
      cl = URLClassLoader.newInstance(jarUrls);
    }

    var target = toCompilerString(i);
    try {
      return cl.loadClass(target);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(
          String.format("Import %s not found in JARS %s", target, jarUrls), e);
    }
  }

  // Assumes that a class always starts with a capital letter
  private static final Pattern CLASS_START_PATTERN = Pattern.compile("\\.[A-Z]");

  // While a subclass `MySubclass` of a class `MyClass` is referenced with `MySubclass.MyClass` in
  // the code, it is `MySubclass$MyClass` in the compiler and therefore finding it in the JAR
  // requires looking for the compiler style import string.
  //
  // In order to convert to one from the other, we rely on the widespread convention that class
  // names start with a capital letter while packages use no capital letters, or at least do not
  // begin with a capital letter.
  //
  // This means that we do not support extending a subclass that does not follow this (good) naming
  // practice.
  private String toCompilerString(Import i) {
    var s = i.selector.toString();
    var matcher = CLASS_START_PATTERN.matcher(s);
    var builder = new StringBuilder(s);
    // Skip the first match as that corresponds to the most parent class
    matcher.results().map(MatchResult::start).skip(1).forEach(idx -> builder.setCharAt(idx, '$'));
    return builder.toString();
  }
}
