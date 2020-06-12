package com.nikodoko.javaimports.stdlib;

import com.google.common.collect.ImmutableMap;
import com.nikodoko.javaimports.parser.Import;
import com.nikodoko.javaimports.stdlib.internal.Stdlib;
import java.util.Map;

public class FakeStdlib implements Stdlib {
  private static final Map<String, Import[]> CLASSES =
      new ImmutableMap.Builder()
          .put(
              "List",
              new Import[] {
                new Import("List", "java.util", false), new Import("List", "java.awt", false)
              })
          .put(
              "Duration",
              new Import[] {
                new Import("Duration", "java.time", false),
                new Import("Duration", "javax.xml.datatype", false)
              })
          .put("Component", new Import[] {new Import("Component", "java.awt", false)})
          .build();

  public Map<String, Import[]> getClasses() {
    return CLASSES;
  }
}
