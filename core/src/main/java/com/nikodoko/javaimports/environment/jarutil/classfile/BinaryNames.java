package com.nikodoko.javaimports.environment.jarutil.classfile;

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
 */
public class BinaryNames {
  // Assumes that a class always starts with a capital letter
  private static final Pattern CLASS_START_PATTERN = Pattern.compile("\\.[A-Z]");
  private static final String SUBCLASS_SEPARATOR = "\\$";
  private static final String DOT = "\\.";
  private static final String SLASH = "\\/";
  private static final String CLASS_EXTENSION = ".class";

  public static String fromSelector(Selector s) {
    var fullyQualifiedName = s.toString();
    var m = CLASS_START_PATTERN.matcher(fullyQualifiedName);
    var b = new StringBuilder(fullyQualifiedName.replaceAll(DOT, SLASH));
    m.results()
        .map(MatchResult::start)
        // Skip the first match as that corresponds to the most parent class
        .skip(1)
        .forEach(idx -> b.setCharAt(idx, SUBCLASS_SEPARATOR.charAt(1)));
    return b.toString();
  }

  public static Selector toSelector(String s) {
    return Selector.of(Arrays.asList(s.replaceAll(SUBCLASS_SEPARATOR, SLASH).split(SLASH)));
  }
}
