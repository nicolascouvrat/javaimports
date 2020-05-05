package com.nikodoko.packagetest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Module {
  private String name;
  private Map<String, String> files;

  public Module(String name, Map<String, String> files) {
    this.name = name;
    this.files = files;
  }

  /** The name of this {@code Module}. */
  public String name() {
    return name;
  }

  /** Returns an iterable view of all the files contained in this module */
  public Iterable<File> files() {
    List<File> l = new ArrayList<>();
    for (Map.Entry<String, String> f : files.entrySet()) {
      l.add(new File(f.getKey(), f.getValue()));
    }

    return l;
  }

  public static class File {
    private String fragment;
    private String content;

    /**
     * A path fragment pointing to this {@code File}.
     *
     * <p>This is essentially the relative path to this file from the module root. For instance, if
     * the folder structure is com/package/prefix/package/File.java, then fragment will be
     * package/File.java.
     */
    public String fragment() {
      return fragment;
    }

    /** This {@code File}'s contents */
    public String content() {
      return content;
    }

    private File(String fragment, String content) {
      this.fragment = fragment;
      this.content = content;
    }
  }
}
