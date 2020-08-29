package com.nikodoko.javaimports.resolver.maven;

class MavenEnvironmentException extends Exception {
  MavenEnvironmentException(String msg, Exception cause) {
    super(msg, cause);
  }
}
