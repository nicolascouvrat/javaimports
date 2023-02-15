package com.nikodoko.javaimports.environment.jarutil;

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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads all public and protected identifiers for a given class (identifier by its {@link Import}).
 */
public class JarIdentifierLoader implements IdentifierLoader {
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
  public Set<Identifier> loadIdentifiers(Import i) {
    var c = loadClass(i);
    var identifiers = new HashSet<Identifier>();
    while (c != null) {
      identifiers.addAll(getUsableDeclaredIdentifiers(c));
      c = c.getSuperclass();
    }

    return identifiers;
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

  private Class loadClass(Import i) {
    if (cl == null) {
      cl = URLClassLoader.newInstance(jarUrls);
    }

    try {
      return cl.loadClass(i.selector.toString());
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(
          String.format("Import %s not found in JARS %s", i, jarUrls), e);
    }
  }
}
