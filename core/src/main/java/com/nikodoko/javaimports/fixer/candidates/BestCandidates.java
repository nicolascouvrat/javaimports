package com.nikodoko.javaimports.fixer.candidates;

import com.google.common.base.MoreObjects;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.Selector;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class BestCandidates {
  private final Map<Selector, Import> imports;

  private BestCandidates(Map<Selector, Import> imports) {
    this.imports = imports;
  }

  public Optional<Import> forSelector(Selector selector) {
    return null;
  }

  static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof BestCandidates)) {
      return false;
    }

    var that = (BestCandidates) o;
    return Objects.equals(this.imports, that.imports);
  }

  @Override
  public int hashCode() {
    return Objects.hash(imports);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("imports", imports).toString();
  }

  static class Builder {
    private Map<Selector, Import> imports = new HashMap<>();

    Builder put(Selector s, Import i) {
      imports.put(s, i);
      return this;
    }

    BestCandidates build() {
      return new BestCandidates(imports);
    }
  }
}
