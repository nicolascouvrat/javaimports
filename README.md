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
  --fix-only
    Do not format ouput, simply add and remove imports.
  --replace, -replace, -r, -w
    Write result to source file instead of stdout.
  --verbose, -verbose, -v
    Verbose logging.
  --version, -version
    Print the version.
  --help, -help, -h
    Print this usage statement.
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
