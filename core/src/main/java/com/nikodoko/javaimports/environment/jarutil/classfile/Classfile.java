package com.nikodoko.javaimports.environment.jarutil.classfile;

import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.Superclass;
import java.io.DataInputStream;
import java.io.IOException;
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

  private static Stream<Identifier> readIdentifiers(DataInputStream dis, ConstantPool cp)
      throws IOException {
    var fields = Fields.readFrom(dis);
    var methods = Fields.readFrom(dis);
    return Stream.concat(
            StreamSupport.stream(fields.spliterator(), false),
            StreamSupport.stream(methods.spliterator(), false))
        .filter(f -> isPublicOrProtected(f.accessFlags()))
        .map(f -> new Identifier(cp.getUtf8Constant(f.nameIdx())));
  }

  private static Stream<Identifier> readInnerClasses(
      DataInputStream dis, ConstantPool cp, int thisClassIdx) throws IOException {
    var attributes = Attributes.readFrom(dis, cp);
    return StreamSupport.stream(attributes.spliterator(), false)
        .filter(Attribute.InnerClasses.class::isInstance)
        .map(a -> (Attribute.InnerClasses) a)
        .flatMap(a -> a.infos().stream())
        // According to the JVM
        // (https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.7.6):
        // "Every CONSTANT_Class_info entry in the constant_pool table which represents a class or
        // interface C that is not a package member must have exactly one corresponding entry in the
        // classes array.
        // If a class or interface has members that are classes or interfaces, its constant_pool
        // table (and hence its InnerClasses attribute) must refer to each such member (JLS §13.1),
        // even if that member is not otherwise mentioned by the class.
        // In addition, the constant_pool table of every nested class and nested interface must
        // refer to its enclosing class, so altogether, every nested class and nested interface will
        // have InnerClasses information for each enclosing class and for each of its own nested
        // classes and interfaces."
        //
        // In other words, the inner classes can contain all enclosing classes in addition to nested
        // classes, which is not something we want to export. We therefore filter based on the outer
        // class to keep only the nested ones.
        .filter(c -> c.outerClassInfoIdx() == thisClassIdx)
        .filter(c -> isPublicOrProtected(c.innerClassAccessFlags()))
        .map(c -> new Identifier(cp.getUtf8Constant(c.innerClassNameIdx())));
  }

  private static boolean isPublicOrProtected(AccessFlags flags) {
    return flags.visibility() == Visibility.PUBLIC || flags.visibility() == Visibility.PROTECTED;
  }

  public static ClassEntity readFrom(DataInputStream s) throws IOException {
    assertClassFile(s);
    skipVersion(s);
    var cp = ConstantPool.readFrom(s);
    var af = AccessFlags.from(s.readShort());

    // This class & parent
    var thisClassIdx = s.readUnsignedShort();
    var thisClass = BinaryNames.toSelector(cp.getClassName(thisClassIdx));
    var parentClass = readParentClass(s, cp);

    // Interfaces
    var il = s.readUnsignedShort();
    for (var i = 0; i < il; i++) {
      s.readUnsignedShort();
    }

    var identifiers = readIdentifiers(s, cp);
    var innerClasses = readInnerClasses(s, cp, thisClassIdx);

    return ClassEntity.named(thisClass)
        .extending(Superclass.resolved(parentClass))
        .declaring(Stream.concat(identifiers, innerClasses).collect(Collectors.toSet()))
        .build();
  }

  private static void assertClassFile(DataInputStream s) throws IOException {
    var m = s.readInt();
    if (m != MAGIC) {
      throw new IllegalArgumentException("File is not a class file");
    }
  }
}
