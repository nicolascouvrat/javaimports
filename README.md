# Javaimports

Javaimports updates Java import lines, adding missing ones.

## Usage

### From the CLI

Run it with:

```
java -jar /path/to/javaimports-1.0-all-deps.jar <options> file
```

## Options

```
Usage: javaimports [options] file

Options:
  --assume-filename, -assume-filename
    File name to use for diagnostics when importing standard input (default is .).
  --fix-only
    Do not format ouput, simply add and remove imports.
  --metrics-datadog-port, -metrics-datadog-port
    Port to use when --metrics-enable is set (default is 8125).
  --metrics-datadog-host, -metrics-datadog-host
    Host to use when --metrics-enable is set (default is "localhost").
  --metrics-enable, -metrics-enable
    Enable metrics reporting to a datadog agent running on the specified port and host.
  --replace, -replace, -r, -w
    Write result to source file instead of stdout.
  --repository, -repository
    Absolute path to the directory containing dependency JARs (default is /your/home/.m2/repository).
  --telemetry-enable, -telemetry-enable
    Enable telemetry. Shorthand for --tracing-enable and --metrics-enable.
  --tracing-enable, -tracing-enable
    Enable tracing reporting to a datadog agent listening at http://localhost:8126.
  --verbose, -verbose, -v
    Verbose logging.
  --version, -version
    Print the version.
  --help, -help, -h
    Print this usage statement.

File:
  setting file equal to '-' will read from stdin
```

## Features

`javaimports` will find imports using the following methods, by order of decreasing priority:

1. mimicking imports from files in the same folder (if you're using `import my.custom.List` in a
   file of the same package, `List<String> test;` will automatically import `my.custom.List`),
2. fetching imports from the standard java library (defined
   [here](https://docs.oracle.com/javase/8/docs/api/allclasses-noframe.html) for java 8 for
   instance).
3. fetching imports from other files in the same project
4. fetching imports from dependencies

For now, the only version of the standard library supported is Java 8. Steps after **3.** use
build-system-specific information, and currently only support Maven.

## Javaimports and `native-image` (experimental)

Javaimports comes with experimental support for
[`native-image`](https://www.graalvm.org/reference-manual/native-image/). This will considerably
speed up execution of `javaimports` (including the formatting if not using `--fix-only`), especially
in projects with few dependencies. 

This comes at the price of version support: `javaimports` built with `native-image` will _not_
support language features of versions > 11, such as Java 14's new `switch` statement. This is
because `javaimports` (like `google-java-format`) relies on the parser provider by the jdk, and the
Graal JDK used for `native-image` is Java 11.

To compile `javaimports` with `native-image`, make sure you are using the right java version
(`graalvm64-11.0.10` for example), then run `mvn package -Pnative-image`. You will find the
executable in `native-image/target/javaimports-native-image`. To run it, you need to pass it the
path to your java home, like so:


```
./javaimports-native-image -Djava.home=/Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/Home [other flags] <file>"
```

**IMPORTANT:**

Versions of `javaimports` newer than  `1.4-SNAPSHOT (rev. 5b66400)` come packaged with a feature
than will read `.jar` files in order to find identifiers provided by parent classes. This feature
will NOT work with `native-image` due to the following:

```
java.net.MalformedURLException: Accessing an URL protocol that was not enabled. The URL protocol jar is not tested and might not work as expected. It can be enabled by adding the --enable-url-protocols=jar option to the native-image command.
```

This is a known limitation, other `javaimports` features will still work.

## Telemetry (traces and metrics)

In order to help contributors debug and find bottlenecks, `javaimports` can optionally emit traces
and metrics that will be picked up by a Datadog agent. This is disabled by default, see options to
enable it.

## Why `javaimports`?

Before developing in Java, I used to work in Go, using VIM. During that time, I learned to love
command line tools such as `gofmt` (an automatic code formatter) and `goimports` (a tool that adds
and removes `import` lines), that allowed me to focus on *what* I was writing and not *how* I was
organizing it. Moving on to Java, I found
[`google-java-format`](https://github.com/google/google-java-format) to be a fit replacement for
`gofmt`, but I unfortunately was not able to find the equivalent of `goimports`. I then decided to
create `javaimports` to fill this void.

## Philosophy

Being a work in progress, any bug report or feature request for `javaimports` is welcome! But there
are a few core principles that guide `javaimports`'s development, from which I won't deviate.
`javaimports` will always be:

### Not requiring compilation of the target file

Unfortunately, compiling Java is often slow. But `javaimports` should be convenient to use as a file
is being written. This means *`javaimports` does not rely on anything being compiled* (except dependencies).
And it will stay that way.

### A stateless CLI utility

Yes, I am aware that some tools exist that rely on a java language server. These are probably more
powerful, but the goal of `javaimports` is to stay *simple* and *stateless*. I feel like most
everyday situations can be resolved without resorting to overly powerful solutions. And when the
need arises, manually writing one import line is still an option :)

### Not interactive

The goal of `javaimports` is to be runnable in the background (every time a file is saved, for
example). For this very reason, everything is decided without user input. This means that ambiguous
names will always be resolved without choice.

For example, consider a project using both JUnit4 and JUnit5. If you write a new file with `@Test`,
`javaimports` has no way to know if it should import `org.junit.Test` or
`org.junit.jupiter.api.Test`. One of the two will be picked arbitrarily!

Of course, the point of `javaimports` is trying to be as smart as possible regarding such conflicts.
It will use all the information at its disposal to make the most sensible decision possible. For
instance, it will look at what you are using in other similar files, what you've already
imported, the other things you need to import, etc.

When there is no such information, the only way is to manually write the import line (`javaimports`
will *never* overwrite import lines). Fortunately, this should not happen too often.

### Only adding import lines

`javaimports` only does one thing, and that is adding missing import lines. It does not format
anything. For convenience, it comes packaged with
[`google-java-format`](https://github.com/google/google-java-format), but you are free to not use it
if you'd like (`--fix-only` option of the CLI).
