package com.nikodoko.javaimports.parser.entities;

/**
 * An {@code Entity} describes a named language entity such as a method (static or no), class or
 * variable. It always comes with a {@link Kind} and a name.
 */
public interface Entity {
  /** An {@code Entity}'s declared name */
  public String name();

  /** The kind of this {@code Entity} */
  public Kind kind();
}
