package com.nikodoko.javaimports.parser.entities;

/** Describes the visibility of a variable, class or method. */
enum Visibility {
  /** The visibility given by {@code public} */
  PUBLIC,
  /** The visibility given by {@code protected} */
  PROTECTED,
  /** The visibility given by {@code private} */
  PRIVATE,
  /** The default visibility for classes (accessible only from the same package) */
  PACKAGE_PRIVATE,
  /** The default visibility for any other scope (accessible only from inside the scope) */
  NONE,
  ;
}
