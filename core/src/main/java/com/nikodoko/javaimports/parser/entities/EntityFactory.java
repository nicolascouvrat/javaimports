package com.nikodoko.javaimports.parser.entities;

import com.nikodoko.javaimports.parser.internal.ClassSelector;
import com.nikodoko.javaimports.parser.internal.ClassSelectors;
import org.openjdk.tools.javac.tree.JCTree.JCExpression;

public class EntityFactory {
  public static ClassEntity createClass(String name) {
    return new ClassEntity(name);
  }

  public static ClassEntity createChildClass(String name, JCExpression extendsClause) {
    ClassSelector superclass = ClassSelectors.of(extendsClause);
    return new ClassEntity(name, superclass);
  }
}
