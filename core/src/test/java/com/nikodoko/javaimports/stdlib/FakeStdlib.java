package com.nikodoko.javaimports.stdlib;

import static com.nikodoko.javaimports.common.CommonTestUtil.anImport;

import com.google.common.collect.ImmutableMap;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.stdlib.internal.Stdlib;
import java.util.Map;

public class FakeStdlib implements Stdlib {
  private static final Map<Identifier, Import[]> CLASSES =
      new ImmutableMap.Builder<Identifier, Import[]>()
          .put(new Identifier("State"), new Import[] {anImport("java.lang.Thread.State")})
          .put(
              new Identifier("Object"),
              new Import[] {anImport("java.lang.Object"), anImport("org.omg.CORBA.Object")})
          .put(
              new Identifier("List"),
              new Import[] {anImport("java.awt.List"), anImport("java.util.List")})
          .put(
              new Identifier("Duration"),
              new Import[] {
                anImport("java.time.Duration"), anImport("javax.xml.datatype.Duration")
              })
          .put(new Identifier("Component"), new Import[] {anImport("java.awt.Component")})
          .build();

  public Import[] getClassesFor(Identifier identifier) {
    return CLASSES.get(identifier);
  }
}
