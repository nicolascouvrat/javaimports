package com.nikodoko.javaimports.parser.entities;

/** Possible kinds of entities */
public enum Kind {
  /** This is used for error handling */
  BAD,
  /** Method */
  METHOD,
  /** Variable */
  VARIABLE,
  /** Type parameter */
  TYPE_PARAMETER,
  /** Class */
  CLASS,
  ;
}
