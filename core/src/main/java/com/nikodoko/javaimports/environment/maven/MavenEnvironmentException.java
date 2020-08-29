package com.nikodoko.javaimports.environment.maven;

class MavenEnvironmentException extends Exception {
  MavenEnvironmentException(String msg, Exception cause) {
    super(msg, cause);
  }
}
