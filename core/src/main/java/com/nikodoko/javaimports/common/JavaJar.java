package com.nikodoko.javaimports.common;

import java.util.Collection;

public interface JavaJar extends ClassProvider, ImportProvider {
  Collection<Import> findAllImports();
}
