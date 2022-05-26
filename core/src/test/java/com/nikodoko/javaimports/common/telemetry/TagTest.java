package com.nikodoko.javaimports.common.telemetry;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

public class TagTest {
  @Test
  public void itShouldWorkWithIntegers() {
    Tag.Key aTagKey = Tag.withKey("test");
    var tag = aTagKey.is("abcdef".length());
    assertThat(tag.toString()).isEqualTo("test:6");
  }
}
