package com.nikodoko.javaimports.environment.jarutil.classfile;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

record Attributes(Attribute[] attributes) implements Iterable<Attribute> {
  static Attributes readFrom(DataInputStream dis) throws IOException {
    var l = dis.readUnsignedShort();
    var attributes = new Attribute[l];
    for (var i = 0; i < l; i++) {
      attributes[i] = Attribute.readFrom(dis);
    }

    return new Attributes(attributes);
  }

  static Attributes readFrom(DataInputStream dis, ConstantPool cp) throws IOException {
    var l = dis.readUnsignedShort();
    var attributes = new Attribute[l];
    for (var i = 0; i < l; i++) {
      attributes[i] = Attribute.readFrom(dis, cp);
    }

    return new Attributes(attributes);
  }

  @Override
  public String toString() {
    return Arrays.toString(attributes);
  }

  @Override
  public Iterator<Attribute> iterator() {
    return Arrays.stream(attributes).iterator();
  }
}
