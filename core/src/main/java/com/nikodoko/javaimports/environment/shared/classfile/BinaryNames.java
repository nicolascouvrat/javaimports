package com.nikodoko.javaimports.environment.shared.classfile;

import com.nikodoko.javaimports.common.Selector;
import java.util.Arrays;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * While a subclass `MySubclass` of a class `MyClass` is referenced with `MySubclass.MyClass` in the
 * code, it is `MySubclass$MyClass` in the compiler and therefore finding it in the JAR requires
 * looking for the compiler style import string.
 *
 * <p>In order to convert to one from the other, we rely on the widespread convention that class
 * names start with a capital letter while packages use no capital letters, or at least do not begin
 * with a capital letter.
 *
 * <p>The difference between {@code byteCodeCompatible} or not is whether the separator used between
 * identifiers should be a dot or a slash. See
 * https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1 and
 * https://docs.oracle.com/javase/specs/jvms/se23/html/jvms-4.html#jvms-4.2
 */
public class BinaryNames {
  // Assumes that a class always starts with a capital letter
  private static final Pattern CLASS_START_PATTERN = Pattern.compile("\\.[A-Z]");
  private static final String SUBCLASS_SEPARATOR = "\\$";
  private static final String DOT = "\\.";
  private static final String SLASH = "\\/";

  public static String fromSelector(Selector s, boolean byteCodeCompatible) {
    var fullyQualifiedName = s.toString();
    var m = CLASS_START_PATTERN.matcher(fullyQualifiedName);
    if (byteCodeCompatible) {
      fullyQualifiedName = fullyQualifiedName.replaceAll(DOT, SLASH);
    }

    var b = new StringBuilder(fullyQualifiedName);
    m.results()
        .map(MatchResult::start)
        // Skip the first match as that corresponds to the most parent class
        .skip(1)
        .forEach(idx -> b.setCharAt(idx, SUBCLASS_SEPARATOR.charAt(1)));
    return b.toString();
  }

  public static Selector toSelector(String s, boolean byteCodeCompatible) {
    var separator = byteCodeCompatible ? SLASH : DOT;
    return Selector.of(Arrays.asList(s.replaceAll(SUBCLASS_SEPARATOR, separator).split(separator)));
  }
}
