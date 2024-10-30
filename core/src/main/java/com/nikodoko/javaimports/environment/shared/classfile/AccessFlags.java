package com.nikodoko.javaimports.environment.shared.classfile;

record AccessFlags(Visibility visibility) {
  static AccessFlags from(short raw) {
    var visibility = Visibility.from(raw);
    return new AccessFlags(visibility);
  }
}
