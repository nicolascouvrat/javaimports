package com.nikodoko.javaimports.environment.jarutil.classfile;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

record ConstantPool(Constant[] constants) implements Iterable<Constant> {
  static ConstantPool readFrom(DataInputStream dis) throws IOException {
    var l = dis.readUnsignedShort();
    var constants = new Constant[l];
    // Entry 0 is unused by the compiler
    for (var i = 1; i < l; i++) {
      var c = Constant.readFrom(dis);
      constants[i] = c;

      // As noted in the JVM's documentation: "All 8-byte constants take up two entries in the
      // constant_pool table of the class file. If a CONSTANT_Long_info or CONSTANT_Double_info
      // structure is the entry at index n in the constant_pool table, then the next usable entry
      // in the table is located at index n+2"
      // See https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.4.5
      if (c.tag() == Constant.Tag.DOUBLE || c.tag() == Constant.Tag.LONG) {
        i++;
      }
    }

    return new ConstantPool(constants);
  }

  @Override
  public Iterator<Constant> iterator() {
    return Arrays.stream(constants).iterator();
  }

  String getUtf8Constant(int idx) {
    var c = constants[idx];
    if (c.tag() != Constant.Tag.UTF8) {
      throw new IllegalArgumentException("constant index %d is not UTF8".formatted(idx));
    }

    return ((Constant.Utf8Info) c).name();
  }

  String getClassName(int idx) {
    var c = constants[idx];
    if (c.tag() != Constant.Tag.CLASS) {
      throw new IllegalArgumentException("constant index %d is not CLASS".formatted(idx));
    }

    return getUtf8Constant(((Constant.ClassInfo) c).nameIdx());
  }
}
