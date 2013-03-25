package org.denis.model;

import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 * @since 3/25/13 6:48 PM
 */
public class FontFamilyName extends AbstractFlyweightInfo {

  public FontFamilyName(int id) {
    super(id);
  }

  @Override
  public void invite(@NotNull OutputInfoVisitor visitor) {
    visitor.visit(this);
  }
}
