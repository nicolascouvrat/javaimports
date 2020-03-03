package com.nikodoko.javaimports.fixer;

import com.nikodoko.javaimports.parser.ParsedFile;
import java.util.Set;

public class Fixer {
  ParsedFile file;
  Set<ParsedFile> siblings;

  private Fixer(ParsedFile file) {
    this.file = file;
  }

  public static Fixer init(ParsedFile file) {
    return new Fixer(file);
  }

  public void addSiblings(Set<ParsedFile> siblings) {
    this.siblings = siblings;
  }

  public Result tryToFix() {
    // TODO: implement
    return null;
  }

  public static class Result {
    private boolean done;

    public boolean done() {
      return done;
    }
  }
}
