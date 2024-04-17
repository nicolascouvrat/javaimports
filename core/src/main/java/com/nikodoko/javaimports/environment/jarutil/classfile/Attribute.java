package com.nikodoko.javaimports.environment.jarutil.classfile;

import java.io.DataInputStream;
import java.io.IOException;

record Attribute(int nameIdx, int length) {
  static Attribute readFrom(DataInputStream dis) throws IOException {
    var nameIdx = dis.readUnsignedShort();
    var length = dis.readInt();
    // We don't care about the details of the attribute
    dis.skipBytes(length);
    return new Attribute(nameIdx, length);
  }
}
