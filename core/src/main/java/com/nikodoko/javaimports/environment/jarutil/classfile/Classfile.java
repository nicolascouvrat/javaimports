package com.nikodoko.javaimports.environment.jarutil.classfile;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Superclass;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Classfile {
  private static final int MAGIC = 0xCAFEBABE;

  private static void skipVersion(DataInputStream dis) throws IOException {
    // version is 4 bytes (2 for major, 2 for minor)
    dis.skipBytes(4);
  }

  private static Selector readClass(DataInputStream dis, ConstantPool cp) throws IOException {
    var idx = dis.readUnsignedShort();
    return BinaryNames.toSelector(cp.getClassName(idx));
  }

  private static Import readParentClass(DataInputStream dis, ConstantPool cp) throws IOException {
    var parentSelector = readClass(dis, cp);
    return new Import(parentSelector, false);
  }

  private static Set<Identifier> readIdentifiers(DataInputStream dis, ConstantPool cp)
      throws IOException {
    var fields = Fields.readFrom(dis);
    var methods = Fields.readFrom(dis);
    return Stream.concat(
            StreamSupport.stream(fields.spliterator(), false),
            StreamSupport.stream(methods.spliterator(), false))
        .filter(
            f ->
                f.accessFlags().visibility() == Visibility.PUBLIC
                    || f.accessFlags().visibility() == Visibility.PROTECTED)
        .map(f -> new Identifier(cp.getUtf8Constant(f.nameIdx())))
        .collect(Collectors.toSet());
  }

  public static ClassEntity readFrom(DataInputStream s) throws IOException {
    assertClassFile(s);
    skipVersion(s);
    var cp = ConstantPool.readFrom(s);
    var af = AccessFlags.from(s.readShort());

    // This class & parent
    var thisClass = readClass(s, cp);
    var parentClass = readParentClass(s, cp);

    // Interfaces
    var il = s.readUnsignedShort();
    for (var i = 0; i < il; i++) {
      s.readUnsignedShort();
    }

    // Fields
    var identifiers = readIdentifiers(s, cp);

    return ClassEntity.named(thisClass)
        .extending(Superclass.resolved(parentClass))
        .declaring(identifiers)
        .build();
  }

  private static void assertClassFile(DataInputStream s) throws IOException {
    var m = s.readInt();
    if (m != MAGIC) {
      throw new IllegalArgumentException("File is not a class file");
    }
  }
}
