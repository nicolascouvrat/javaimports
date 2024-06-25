package com.nikodoko.javaimports.environment.jarutil.classfile;

import java.io.DataInputStream;
import java.io.IOException;

interface Constant {
  static class Tag {
    static final byte UTF8 = 1;
    static final byte INTEGER = 3;
    static final byte FLOAT = 4;
    static final byte LONG = 5;
    static final byte DOUBLE = 6;
    static final byte CLASS = 7;
    static final byte STRING = 8;
    static final byte FIELD_REF = 9;
    static final byte METHOD_REF = 10;
    static final byte INTERFACE_METHOD_REF = 11;
    static final byte NAME_AND_TYPE = 12;
    static final byte METHOD_HANDLE = 15;
    static final byte METHOD_TYPE = 16;
    static final byte DYNAMIC = 17;
    static final byte INVOKE_DYNAMIC = 18;
    static final byte MODULE = 19;
    static final byte PACKAGE = 20;
  }

  byte tag();

  static Constant readFrom(DataInputStream dis) throws IOException {
    var tag = dis.readByte();
    switch (tag) {
      case Tag.UTF8:
        return new Utf8Info(dis);
      case Tag.INTEGER:
        return new IntegerInfo(dis);
      case Tag.FLOAT:
        return new FloatInfo(dis);
      case Tag.LONG:
        return new LongInfo(dis);
      case Tag.DOUBLE:
        return new DoubleInfo(dis);
      case Tag.CLASS:
        return new ClassInfo(dis);
      case Tag.STRING:
        return new StringInfo(dis);
      case Tag.FIELD_REF:
        return new FieldRefInfo(dis);
      case Tag.METHOD_REF:
        return new MethodRefInfo(dis);
      case Tag.INTERFACE_METHOD_REF:
        return new InterfaceMethodRefInfo(dis);
      case Tag.NAME_AND_TYPE:
        return new NameAndTypeInfo(dis);
      case Tag.METHOD_HANDLE:
        return new MethodHandleInfo(dis);
      case Tag.METHOD_TYPE:
        return new MethodTypeInfo(dis);
      case Tag.DYNAMIC:
        return new DynamicInfo(dis);
      case Tag.INVOKE_DYNAMIC:
        return new InvokeDynamicInfo(dis);
      case Tag.MODULE:
        return new ModuleInfo(dis);
      case Tag.PACKAGE:
        return new PackageInfo(dis);
      default:
        throw new IllegalArgumentException("Unknown constant tag: " + tag);
    }
  }

  record Utf8Info(byte tag, String name) implements Constant {
    private Utf8Info(DataInputStream dis) throws IOException {
      this(Tag.UTF8, dis.readUTF());
    }
  }

  record IntegerInfo(byte tag, int value) implements Constant {
    private IntegerInfo(DataInputStream dis) throws IOException {
      this(Tag.INTEGER, dis.readInt());
    }
  }

  record FloatInfo(byte tag, float value) implements Constant {
    private FloatInfo(DataInputStream dis) throws IOException {
      this(Tag.FLOAT, dis.readFloat());
    }
  }

  record LongInfo(byte tag, long value) implements Constant {
    private LongInfo(DataInputStream dis) throws IOException {
      this(Tag.LONG, dis.readLong());
    }
  }

  record DoubleInfo(byte tag, double value) implements Constant {
    private DoubleInfo(DataInputStream dis) throws IOException {
      this(Tag.DOUBLE, dis.readDouble());
    }
  }

  record ClassInfo(byte tag, int nameIdx) implements Constant {
    private ClassInfo(DataInputStream dis) throws IOException {
      this(Tag.CLASS, dis.readUnsignedShort());
    }
  }

  record StringInfo(byte tag, int stringIdx) implements Constant {
    private StringInfo(DataInputStream dis) throws IOException {
      this(Tag.STRING, dis.readUnsignedShort());
    }
  }

  record FieldRefInfo(byte tag, int classIdx, int nameAndTypeIdx) implements Constant {
    private FieldRefInfo(DataInputStream dis) throws IOException {
      this(Tag.FIELD_REF, dis.readUnsignedShort(), dis.readUnsignedShort());
    }
  }

  record MethodRefInfo(byte tag, int classIdx, int nameAndTypeIdx) implements Constant {
    private MethodRefInfo(DataInputStream dis) throws IOException {
      this(Tag.METHOD_REF, dis.readUnsignedShort(), dis.readUnsignedShort());
    }
  }

  record InterfaceMethodRefInfo(byte tag, int classIdx, int nameAndTypeIdx) implements Constant {
    private InterfaceMethodRefInfo(DataInputStream dis) throws IOException {
      this(Tag.INTERFACE_METHOD_REF, dis.readUnsignedShort(), dis.readUnsignedShort());
    }
  }

  record NameAndTypeInfo(byte tag, int nameIdx, int descriptorIdx) implements Constant {
    private NameAndTypeInfo(DataInputStream dis) throws IOException {
      this(Tag.NAME_AND_TYPE, dis.readUnsignedShort(), dis.readUnsignedShort());
    }
  }

  record MethodHandleInfo(byte tag, byte referenceKind, int referenceIdx) implements Constant {
    private MethodHandleInfo(DataInputStream dis) throws IOException {
      this(Tag.METHOD_HANDLE, dis.readByte(), dis.readUnsignedShort());
    }
  }

  record MethodTypeInfo(byte tag, int descriptorIdx) implements Constant {
    private MethodTypeInfo(DataInputStream dis) throws IOException {
      this(Tag.METHOD_HANDLE, dis.readUnsignedShort());
    }
  }

  record DynamicInfo(byte tag, int bootstrapMethodAttrIdx, int nameAndTypeIdx) implements Constant {
    private DynamicInfo(DataInputStream dis) throws IOException {
      this(Tag.DYNAMIC, dis.readUnsignedShort(), dis.readUnsignedShort());
    }
  }

  record InvokeDynamicInfo(byte tag, int bootstrapMethodAttrIdx, int nameAndTypeIdx)
      implements Constant {
    private InvokeDynamicInfo(DataInputStream dis) throws IOException {
      this(Tag.INVOKE_DYNAMIC, dis.readUnsignedShort(), dis.readUnsignedShort());
    }
  }

  record ModuleInfo(byte tag, int nameIdx) implements Constant {
    private ModuleInfo(DataInputStream dis) throws IOException {
      this(Tag.MODULE, dis.readUnsignedShort());
    }
  }

  record PackageInfo(byte tag, int nameIdx) implements Constant {
    private PackageInfo(DataInputStream dis) throws IOException {
      this(Tag.PACKAGE, dis.readUnsignedShort());
    }
  }
}
