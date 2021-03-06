package com.nikodoko.javaimports.common;

import java.util.Set;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.ArbitraryProvider;
import net.jqwik.api.providers.TypeUsage;

public class ArbitrarySelectorProvider implements ArbitraryProvider {
  @Override
  public boolean canProvideFor(TypeUsage target) {
    return target.isOfType(Selector.class);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(TypeUsage target, ArbitraryProvider.SubtypeProvider __) {
    return Set.of(CommonTestUtil.arbitrarySelector());
  }
}
