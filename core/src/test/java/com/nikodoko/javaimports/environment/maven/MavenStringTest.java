package com.nikodoko.javaimports.environment.maven;

import static com.google.common.truth.Truth.assertThat;

import java.util.Map;
import java.util.Properties;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

public class MavenStringTest {
  @Example
  void it_should_find_multiple_properties() {
    var s = new MavenString("akka-${akka-scala.type}_${akka-scala.version}");
    assertThat(s.propertyReferences()).containsExactly("akka-scala.type", "akka-scala.version");
  }

  @Property
  void it_should_handle_any_string(@ForAll String aString) {
    assertThat(new MavenString(aString).toString()).isEqualTo(aString);
  }

  @Example
  void it_can_substitute_props() {
    var s = new MavenString("akka-${akka-scala.type}_${akka-scala.version}");
    var props = new Properties();
    props.setProperty("akka-scala.version", "1.0.0");

    s.substitute(props);
    assertThat(s.toString()).isEqualTo("akka-${akka-scala.type}_1.0.0");

    props.setProperty("akka-scala.type", "actor");
    s.substitute(props);
    assertThat(s.toString()).isEqualTo("akka-actor_1.0.0");
  }

  @Example
  void it_can_substitute_indirect_props() {
    var s = new MavenString("akka-${akka-scala.type}_${akka-scala.version}");
    var props = new Properties();
    props.setProperty("akka-scala.version", "${akka.version}");
    props.setProperty("akka-scala.type", "${akka.type}");
    props.setProperty("akka.type", "actor");

    s.substitute(props);
    assertThat(s.toString()).isEqualTo("akka-actor_${akka.version}");

    props.setProperty("akka.version", "1.0.0");
    s.substitute(props);
    assertThat(s.toString()).isEqualTo("akka-actor_1.0.0");
  }

  @Property
  void it_can_handle_any_properties(
      @ForAll Map<String, String> someProperties, @ForAll String aString) {
    var props = new Properties();
    someProperties.entrySet().stream().forEach(e -> props.setProperty(e.getKey(), e.getValue()));
    // This should not error
    new MavenString(aString).substitute(props);
  }
}
