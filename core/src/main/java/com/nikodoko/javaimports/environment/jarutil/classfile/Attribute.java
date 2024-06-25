package com.nikodoko.javaimports.environment.jarutil.classfile;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

interface Attribute {
  static class Type {
    static final String INNER_CLASSES = "InnerClasses";
  }

  int nameIdx();

  int length();

  record Untyped(int nameIdx, int length) implements Attribute {
    static Untyped readFrom(DataInputStream dis) throws IOException {
      var nameIdx = dis.readUnsignedShort();
      var length = dis.readInt();
      return new Untyped(nameIdx, length);
    }
  }

  record InnerClassInfo(
      int innerClassInfoIdx,
      int outerClassInfoIdx,
      int innerClassNameIdx,
      AccessFlags innerClassAccessFlags) {
    static InnerClassInfo readFrom(DataInputStream dis) throws IOException {
      return new InnerClassInfo(
          dis.readUnsignedShort(),
          dis.readUnsignedShort(),
          dis.readUnsignedShort(),
          AccessFlags.from(dis.readShort()));
    }
  }

  record InnerClasses(int nameIdx, int length, int numberOfClasses, List<InnerClassInfo> infos)
      implements Attribute {
    static InnerClasses readFrom(Untyped base, DataInputStream dis) throws IOException {
      var numberOfClasses = dis.readUnsignedShort();
      var infos = new ArrayList<InnerClassInfo>(numberOfClasses);
      for (var i = 0; i < numberOfClasses; i++) {
        infos.add(InnerClassInfo.readFrom(dis));
      }

      return new InnerClasses(base.nameIdx(), base.length(), numberOfClasses, infos);
    }
  }

  /**
   * Read attributes without caring about what type they are, as this requires knowing the constant
   * pool.
   */
  static Attribute readFrom(DataInputStream dis) throws IOException {
    var untyped = Untyped.readFrom(dis);
    // We don't care about the details of the attribute
    dis.skipBytes(untyped.length());
    return untyped;
  }

  /** Read attributes, enriching them with constant pool info. */
  static Attribute readFrom(DataInputStream dis, ConstantPool cp) throws IOException {
    var untyped = Untyped.readFrom(dis);
    switch (cp.getUtf8Constant(untyped.nameIdx())) {
      case Type.INNER_CLASSES:
        return InnerClasses.readFrom(untyped, dis);
      default:
        // We don't care about the details of the attribute
        dis.skipBytes(untyped.length());
        return untyped;
    }
  }
}
