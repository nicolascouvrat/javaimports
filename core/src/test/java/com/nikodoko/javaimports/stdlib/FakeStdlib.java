package com.nikodoko.javaimports.stdlib;

import com.google.common.collect.ImmutableMap;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.stdlib.internal.Stdlib;
import java.util.Map;

public class FakeStdlib implements Stdlib {
  private static final Map<String, Import[]> CLASSES =
      new ImmutableMap.Builder<String, Import[]>()
          .put(
              "State",
              new Import[] {
                new Import("State", "java.lang.Thread", false),
              })
          .put(
              "Object",
              new Import[] {
                new Import("Object", "java.lang", false),
                new Import("Object", "org.omg.CORBA", false)
              })
          .put(
              "List",
              new Import[] {
                new Import("List", "java.awt", false), new Import("List", "java.util", false)
              })
          .put(
              "Duration",
              new Import[] {
                new Import("Duration", "java.time", false),
                new Import("Duration", "javax.xml.datatype", false)
              })
          .put("Component", new Import[] {new Import("Component", "java.awt", false)})
          .build();

  public Import[] getClassesFor(String identifier) {
    return CLASSES.get(identifier);
  }
}
