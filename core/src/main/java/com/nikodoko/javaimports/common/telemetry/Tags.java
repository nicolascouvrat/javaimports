package com.nikodoko.javaimports.common.telemetry;

/** A repository of common tags. */
public class Tags {
  public static Tag.Key VERSION = Tag.withKey("version");
  public static Tag VERSION_TAG = VERSION.is(Traces.class.getPackage().getImplementationVersion());
}
