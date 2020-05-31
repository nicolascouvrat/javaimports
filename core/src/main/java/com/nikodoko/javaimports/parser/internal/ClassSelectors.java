package com.nikodoko.javaimports.parser.internal;

import java.util.Optional;

public class ClassSelectors {
  public static ClassSelector of(String first, String... others) {
    Selector head = new Selector(first);
    Selector tail = head;
    for (String other : others) {
      Selector s = new Selector(other);
      tail.next = s;
      tail = s;
    }

    return head;
  }

  static class Selector implements ClassSelector {
    String selector;
    ClassSelector next;

    Selector(String selector) {
      this.selector = selector;
    }

    @Override
    public String selector() {
      return selector;
    }

    @Override
    public Optional<ClassSelector> next() {
      return Optional.ofNullable(next);
    }
  }
}
