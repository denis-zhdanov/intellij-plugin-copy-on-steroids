package org.denis.model;

import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 * @since 3/25/13 11:23 PM
 */
public class FontSize implements OutputInfo {
  
  private final int mySize;

  public FontSize(int size) {
    mySize = size;
  }

  public int getSize() {
    return mySize;
  }

  @Override
  public void invite(@NotNull OutputInfoVisitor visitor) {
    visitor.visit(this);
  }
}
