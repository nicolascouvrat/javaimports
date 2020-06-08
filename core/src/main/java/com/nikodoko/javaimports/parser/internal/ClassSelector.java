package com.nikodoko.javaimports.parser.internal;

import java.util.Optional;

/**
 * A representation of the extends clause of Java classes.
 *
 * <p>For example, {@code A extends B.C} would result in a {@code ClassSelector} with {@code
 * selector() == "B"}, pointing to a {@code ClassSelector} with {@code selector() == "C"}.
 */
public interface ClassSelector {
  public String selector();

  public Optional<ClassSelector> next();
}
