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

  @Override
  public int hashCode() {
    return mySize;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FontSize size = (FontSize)o;

    return mySize == size.mySize;
  }

  @Override
  public String toString() {
    return "font size=" + mySize;
  }
}
