package com.nikodoko.packagetest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A system agnostic description of a java module.
 *
 * <p>For example, consider the following structure:
 *
 * <pre>
 * com
 *  |
 *  - package
 *    |
 *    - name
 *      |
 *      - A.java
 *      - util
 *        |
 *        - B.java
 * </pre>
 *
 * This can be described as a {@code Module} of name {@code "com.package.name"} and containing two
 * {@link File} designated by their path fragment ({@code "A.java"} for one and {@code
 * "util/B.java"}) for the other.
 */
public class Module {
  private String name;
  private Map<String, String> files;

  /**
   * A {@code Module} constructor.
   *
   * @param name its name
   * @param files the files it contains
   */
  public Module(String name, Map<String, String> files) {
    this.name = name;
    this.files = files;
  }

  /** The name of this {@code Module}. */
  public String name() {
    return name;
  }

  /** Returns an iterable view of all the files contained in this module. */
  public Iterable<File> files() {
    List<File> l = new ArrayList<>();
    for (Map.Entry<String, String> f : files.entrySet()) {
      l.add(new File(f.getKey(), f.getValue()));
    }

    return l;
  }

  /** A system agnostic description of a file in a Java module. */
  public static class File {
    private String fragment;
    private String content;

    /**
     * A path fragment pointing to this {@code File}.
     *
     * <p>This is essentially the relative path to this file from the module root. For instance, if
     * the folder structure is {@code com/package/prefix/package/File.java}, then {@code fragment()}
     * will return {@code "package/File.java"}.
     */
    public String fragment() {
      return fragment;
    }

    /** This {@code File}'s contents. */
    public String content() {
      return content;
    }

    private File(String fragment, String content) {
      this.fragment = fragment;
      this.content = content;
    }
  }
}
