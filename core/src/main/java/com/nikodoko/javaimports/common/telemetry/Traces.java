package com.nikodoko.javaimports.common.telemetry;

import datadog.opentracing.DDTracer;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.noop.NoopScopeManager.NoopScope;
import io.opentracing.noop.NoopSpan;
import io.opentracing.util.GlobalTracer;
import java.util.List;

public class Traces {
  private static volatile boolean enabled = false;
  private static final List<Tag> defaultTags =
      List.of(new Tag("version", Traces.class.getPackage().getImplementationVersion()));

  public static void enable() {
    if (enabled) {
      return;
    }

    turnOn();
  }

  private static synchronized void turnOn() {
    if (enabled) {
      return;
    }

    var ddTracer = DDTracer.builder().build();
    GlobalTracer.register(ddTracer);
    datadog.trace.api.GlobalTracer.registerIfAbsent(ddTracer);
    enabled = true;
  }

  public static void close() {
    if (!enabled) {
      return;
    }

    turnOff();
  }

  private static synchronized void turnOff() {
    if (!enabled) {
      return;
    }

    var tracer = GlobalTracer.get();
    tracer.close();
    enabled = false;
  }

  public static Span createSpan(String name, Tag... tags) {
    if (!enabled) {
      return NoopSpan.INSTANCE;
    }

    var builder = GlobalTracer.get().buildSpan(name);
    builder = decorate(builder);
    return builder.start();
  }

  private static SpanBuilder decorate(SpanBuilder spanBuilder, Tag... tags) {
    for (var tag : defaultTags) {
      spanBuilder.withTag(tag.key, tag.value);
    }

    for (var tag : tags) {
      spanBuilder.withTag(tag.key, tag.value);
    }

    return spanBuilder;
  }

  public static Scope activate(Span span) {
    if (!enabled) {
      return NoopScope.INSTANCE;
    }

    return GlobalTracer.get().activateSpan(span);
  }
}
