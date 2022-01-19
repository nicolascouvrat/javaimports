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
  private Set<String> propertyReferences = new HashSet<>();

  public MavenString(String template) {
    this.template = template;
    var m = MAVEN_PROPERTY_PATTERN.matcher(template);
    while (m.find()) {
      propertyReferences.add(m.group("property"));
    }
  }

  public Set<String> propertyReferences() {
    return propertyReferences;
  }

  public void substitute(Properties props) {
    var it = propertyReferences.iterator();
    while (it.hasNext()) {
      var ref = it.next();
      var value = props.getProperty(ref);
      if (value != null) {
        substitute(ref, props.getProperty(ref));
        it.remove();
      }
    }
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
