package com.nikodoko.javaimports.resolver.maven;

class ProjectScannerException extends Exception {
  ProjectScannerException(String msg, Exception cause) {
    super(msg, cause);
  }
}
