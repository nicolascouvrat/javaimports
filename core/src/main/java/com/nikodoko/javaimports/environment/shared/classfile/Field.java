package com.nikodoko.javaimports.environment.shared.classfile;

import java.io.DataInputStream;
import java.io.IOException;

record Field(
    AccessFlags accessFlags,
    int nameIdx,
    int descriptorIdx,
    int attributesCount,
    Attributes attributes) {
  static Field readFrom(DataInputStream dis) throws IOException {
    var accessFlags = AccessFlags.from(dis.readShort());
    var nameIdx = dis.readUnsignedShort();
    var descriptorIdx = dis.readUnsignedShort();
    var attributes = Attributes.readFrom(dis);

    return new Field(
        accessFlags, nameIdx, descriptorIdx, attributes.attributes().length, attributes);
  }
}
