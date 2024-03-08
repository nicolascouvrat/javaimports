package com.nikodoko.javaimports.environment.maven;

import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/** A {@code MavenString} is a string with (optional) property references. */
public class MavenString {
  private static final Pattern MAVEN_PROPERTY_PATTERN =
      Pattern.compile("\\$\\{(?<property>[^{}]+)\\}");
  private String template;
  private volatile Set<String> propertyReferences = new HashSet<>();

  public MavenString(String template) {
    this.template = template;
    this.propertyReferences = extractPropertyReferences(template);
  }

  public Set<String> propertyReferences() {
    return propertyReferences;
  }

  public boolean hasPropertyReferences() {
    return !propertyReferences.isEmpty();
  }

  private Set<String> extractPropertyReferences(String s) {
    var props = new HashSet<String>();
    var m = MAVEN_PROPERTY_PATTERN.matcher(s);
    while (m.find()) {
      props.add(m.group("property"));
    }

    return props;
  }

  public synchronized void substitute(Properties props) {
    var shouldContinue = false;
    do {
      shouldContinue = substituteOnce(props);
    } while (shouldContinue);
  }

  // returns true if we found anything
  private boolean substituteOnce(Properties props) {
    var foundSome = false;
    Set<String> remaining = new HashSet<>();

    for (var ref : propertyReferences) {
      var value = props.getProperty(ref);
      if (value == null) {
        remaining.add(ref);
        continue;
      }

      foundSome = true;
      var extraRefs = extractPropertyReferences(value);
      substitute(ref, value);
      remaining.addAll(extraRefs);
    }

    propertyReferences = remaining;
    return foundSome;
  }

  private void substitute(String prop, String value) {
    template = template.replace(String.format("${%s}", prop), value);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof MavenString)) {
      return false;
    }

    var that = (MavenString) o;
    return Objects.equals(this.template, that.template)
        && Objects.equals(this.propertyReferences, that.propertyReferences);
  }

  @Override
  public int hashCode() {
    return Objects.hash(template, propertyReferences);
  }

  @Override
  public String toString() {
    return template;
  }
}
