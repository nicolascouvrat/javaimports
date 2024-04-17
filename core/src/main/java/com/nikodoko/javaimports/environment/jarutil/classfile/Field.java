package com.nikodoko.javaimports.environment.jarutil.classfile;

import java.io.DataInputStream;
import java.io.IOException;

record Field(
    AccessFlags accessFlags,
    int nameIdx,
    int descriptorIdx,
    int attributesCount,
    Attribute[] attributes) {
  static Field readFrom(DataInputStream dis) throws IOException {
    var accessFlags = AccessFlags.from(dis.readShort());
    var nameIdx = dis.readUnsignedShort();
    var descriptorIdx = dis.readUnsignedShort();
    var attributesCount = dis.readUnsignedShort();
    var attributes = new Attribute[attributesCount];
    for (var i = 0; i < attributesCount; i++) {
      attributes[i] = Attribute.readFrom(dis);
    }

    return new Field(accessFlags, nameIdx, descriptorIdx, attributesCount, attributes);
  }
}
