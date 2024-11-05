package com.nikodoko.javaimports.stdlib;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.ClassProvider;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.environment.shared.classfile.BinaryNames;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: it is not ideal to rely on a class that was made for jar parsing here, but it provides us
// with a temporary solution
public class BasicStdlibClassLibrary implements ClassProvider {
  private static Logger log = Logs.getLogger(BasicStdlibClassLibrary.class.getName());
  private final ClassLoader cl;

  BasicStdlibClassLibrary() {
    cl = URLClassLoader.newInstance(new URL[] {});
  }

  @Override
  public Optional<ClassEntity> findClass(Import i) {
    var target = BinaryNames.fromSelector(i.selector, false);
    try {
      var identifiers = getUsableDeclaredIdentifiers(cl.loadClass(target));
      return Optional.of(ClassEntity.named(i.selector).declaring(identifiers).build());
    } catch (ClassNotFoundException e) {
      log.info(String.format("class not find in stdlib: %s", i));
      return Optional.empty();
    }
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
        .filter(BasicStdlibClassLibrary::isUsableIdentifier)
        .map(IdentifierAndModifier::identifier)
        .collect(Collectors.toSet());
  }

  private static boolean isUsableIdentifier(IdentifierAndModifier im) {
    var modifiers = im.modifiers();
    return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
  }
}
