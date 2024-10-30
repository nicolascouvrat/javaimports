package com.nikodoko.javaimports.environment.shared;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.nikodoko.javaimports.ImporterException;
import com.nikodoko.javaimports.common.ClassEntity;
import com.nikodoko.javaimports.common.Identifier;
import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.common.JavaSourceFile;
import com.nikodoko.javaimports.common.Selector;
import com.nikodoko.javaimports.common.telemetry.Logs;
import com.nikodoko.javaimports.parser.ParsedFile;
import com.nikodoko.javaimports.parser.Parser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public interface LazyParsedFile extends JavaSourceFile {
  CompletableFuture<Void> parseAsync(Executor e);

  @FunctionalInterface
  interface Factory {
    LazyParsedFile build(Selector refPkg, Path filename);
  }

  public static LazyParsedFile of(Selector refPkg, Path filename) {
    return new Impl(refPkg, filename);
  }

  public static class Impl implements LazyParsedFile {
    private static final Logger log = Logs.getLogger(LazyParsedFile.class.getName());

    private final Selector refPkg;
    private final Path filename;
    private final Import inferredImport;

    private volatile Optional<ParsedFile> parsed;

    private Impl(Selector refPkg, Path filename) {
      this.refPkg = refPkg;
      this.filename = filename;
      this.inferredImport = inferImport(filename);
    }

    private static final Pattern SRC_FILE_PATTERN =
        Pattern.compile("^.*src/\\w+/java/(?<selector>.+)\\.java");

    private Import inferImport(Path filename) {
      var matcher = SRC_FILE_PATTERN.matcher(filename.toString());
      if (matcher.matches()) {
        var s = Selector.of(Arrays.asList(matcher.group("selector").split("/")));
        return new Import(s, false);
      }

      log.warning("Cannot infer import for file %s".formatted(filename));
      return null;
    }

    @Override
    public Set<Identifier> topLevelDeclarations() {
      if (parsed == null) {
        if (inferredImport == null) {
          return Set.of();
        }

        return Set.of(inferredImport.selector.identifier());
      }

      return parsed.map(p -> p.topLevelDeclarations()).orElse(Set.of());
    }

    @Override
    public Selector pkg() {
      if (parsed == null) {
        if (inferredImport == null) {
          return Selector.of("");
        }

        return inferredImport.selector.scope();
      }

      return parsed.map(p -> p.pkg()).orElse(Selector.of(""));
    }

    @Override
    public Collection<Import> findImports(Identifier i) {
      if (parsed == null) {
        if (inferredImport == null || !inferredImport.selector.identifier().equals(i)) {
          return List.of();
        }

        return List.of(inferredImport);
      }

      return parsed.map(p -> p.findImports(i)).orElse(List.of());
    }

    @Override
    public Optional<ClassEntity> findClass(Import i) {
      if (parsed != null) {
        return parsed.flatMap(p -> p.findClass(i));
      }

      if (inferredImport == null) {
        return Optional.empty();
      }

      if (!i.equals(inferredImport)) {
        return Optional.empty();
      }

      parse();
      return parsed.flatMap(p -> p.findClass(i));
    }

    @Override
    public CompletableFuture<Void> parseAsync(Executor e) {
      if (parsed != null) {
        return CompletableFuture.completedFuture(null);
      }

      return CompletableFuture.runAsync(this::parse, e);
    }

    private synchronized void parse() {
      synchronized (this) {
        if (parsed != null) {
          return;
        }

        try {
          parsed = parse(refPkg, filename);
        } catch (IOException | ImporterException e) {
          log.log(Level.WARNING, "Could not parse file %s".formatted(filename), e);
          parsed = Optional.empty();
        }
      }
    }

    private static Optional<ParsedFile> parse(Selector refPkg, Path filename)
        throws IOException, ImporterException {
      var src = new String(Files.readAllBytes(filename), UTF_8);
      return new Parser().parse(filename, src, refPkg);
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }

      if (!(o instanceof Impl)) {
        return false;
      }

      var that = (Impl) o;
      return Objects.equals(this.filename, that.filename);
    }

    @Override
    public int hashCode() {
      return Objects.hash(filename);
    }
  }
}
