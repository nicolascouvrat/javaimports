package com.nikodoko.javaimports.resolver.maven;

class MavenProjectParserException extends Exception {
  MavenProjectParserException(String msg, Exception cause) {
    super(msg, cause);
  }
}
