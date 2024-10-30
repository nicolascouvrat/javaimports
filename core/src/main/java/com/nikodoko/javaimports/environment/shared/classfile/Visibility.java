package com.nikodoko.javaimports.environment.shared.classfile;

enum Visibility {
  PUBLIC,
  PRIVATE,
  PROTECTED,
  UNSET;

  private static final short ACC_PUBLIC = 0x0001;
  private static final short ACC_PRIVATE = 0x0002;
  private static final short ACC_PROTECTED = 0x0004;

  static Visibility from(short raw) {
    if ((raw & ACC_PUBLIC) != 0) {
      return PUBLIC;
    }

    if ((raw & ACC_PRIVATE) != 0) {
      return PRIVATE;
    }

    if ((raw & ACC_PROTECTED) != 0) {
      return PROTECTED;
    }

    return UNSET;
  }
}
