package com.nikodoko.javaimports.stdlib;

import com.nikodoko.javaimports.common.ClassProvider;
import com.nikodoko.javaimports.common.ImportProvider;

public interface StdlibProvider extends ImportProvider, ClassProvider {
  public boolean isInJavaLang(String identifier);
}
