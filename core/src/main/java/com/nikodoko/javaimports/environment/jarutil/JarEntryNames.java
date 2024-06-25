package com.nikodoko.javaimports.environment.jarutil;

import com.nikodoko.javaimports.common.Import;
import com.nikodoko.javaimports.environment.jarutil.classfile.BinaryNames;

class JarEntryNames {
  private static final String CLASS_EXTENSION = ".class";
  private static final String JAVA_9_MODULE_INFO = "module-info.class";

  static String fromImport(Import i) {
    var binaryName = BinaryNames.fromSelector(i.selector);
    return binaryName + CLASS_EXTENSION;
  }

  static Import toImport(String s) {
    var selector = BinaryNames.toSelector(s.replace(CLASS_EXTENSION, ""));
    return new Import(selector, false);
  }

  // TODO: we could be smarter and parse the module-info file to know what to import and what to
  // ignore.
  static boolean isImportable(String s) {
    return s.endsWith(CLASS_EXTENSION) && !s.equals(JAVA_9_MODULE_INFO);
  }
}
