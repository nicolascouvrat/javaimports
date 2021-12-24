package com.nikodoko.javaimports.environment.jarutil;

import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.environment.common.IdentifierLoader;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads all public and protected identifiers for a given class (identifier by its {@link Import}).
 */
public class JarIdentifierLoader implements IdentifierLoader {
  ClassLoader cl = null;
  final URL jarUrl;

  public JarIdentifierLoader(Path jarPath) {
    try {
      this.jarUrl = new URL("jar:file:" + jarPath + "!/");
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("could not form an URL for jar path: " + jarPath);
    }
  }

  @Override
  public Set<String> loadIdentifiers(Import i) {
    var c = loadClass(i);
    var identifiers = new HashSet<String>();
    while (c != null) {
      identifiers.addAll(getUsableDeclaredIdentifiers(c));
      c = c.getSuperclass();
    }

    return identifiers;
  }

  private static Set<String> getUsableDeclaredIdentifiers(Class c) {
    var fields = Arrays.stream(c.getDeclaredFields());
    var methods = Arrays.stream(c.getDeclaredMethods());

    return Stream.concat(fields.map(Member.class::cast), methods.map(Member.class::cast))
        .filter(JarIdentifierLoader::isUsableIdentifier)
        .map(Member::getName)
        .collect(Collectors.toSet());
  }

  private static boolean isUsableIdentifier(Member m) {
    var modifiers = m.getModifiers();
    return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
  }

  private Class loadClass(Import i) {
    if (cl == null) {
      cl = URLClassLoader.newInstance(new URL[] {jarUrl});
    }

    try {
      return cl.loadClass(i.selector.toString());
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(String.format("Import %s not found in JAR %s", i, jarUrl));
    }
  }
}
