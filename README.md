# Javaimports

Javaimports updates Java import lines, adding missing ones.

## Usage

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

For now, the only version of the standard library supported is Java 8.

## Credit

This tool is inspired by [`goimports`](https://godoc.org/golang.org/x/tools/cmd/goimports) and [`google-java-format`](https://github.com/google/google-java-format).
