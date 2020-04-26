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

## Credit

This tool is inspired by [`goimports`](https://godoc.org/golang.org/x/tools/cmd/goimports) and [`google-java-format`](https://github.com/google/google-java-format).
