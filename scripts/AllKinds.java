package com.nikodoko.javaimports;

/** Contains various kind of (exported and non exported) symbols. */
public class AllKinds {
  public static class InnerClass {
    public static final int INNER_CONST = 0;
    private static final int INNER_PRIVATE_CONST = 0;

    public static void staticInnerMethod() {}

    private void innerPrivateMethod() {}
  }

  public static final int CONST = 0;

  private int field;

  public void publicMethod() {}
}
