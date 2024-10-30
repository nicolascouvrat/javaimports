package com.nikodoko.javaimports.environment.shared.classfile;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

record Fields(Field[] fields) implements Iterable<Field> {
  static Fields readFrom(DataInputStream dis) throws IOException {
    var l = dis.readUnsignedShort();
    var fields = new Field[l];
    for (var i = 0; i < l; i++) {
      fields[i] = Field.readFrom(dis);
    }

    return new Fields(fields);
  }

  @Override
  public String toString() {
    return fields.length + Arrays.toString(fields);
  }

  @Override
  public Iterator<Field> iterator() {
    return Arrays.stream(fields).iterator();
  }
}
