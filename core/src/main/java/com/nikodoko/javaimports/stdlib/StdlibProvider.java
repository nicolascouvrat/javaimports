package com.nikodoko.javaimports.stdlib;

import com.nikodoko.javaimports.common.ImportProvider;
import com.nikodoko.javaimports.parser.Import;
import java.util.Map;

public interface StdlibProvider extends ImportProvider {
  public Map<String, Import> find(Iterable<String> identifiers);

  public boolean isInJavaLang(String identifier);
}
