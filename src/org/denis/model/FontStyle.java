package org.denis.model;

import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 * @since 3/25/13 7:10 PM
 */
public class FontStyle implements OutputInfo {
  
  private final int myStyle;

  public FontStyle(int style) {
    myStyle = style;
  }

  public int getStyle() {
    return myStyle;
  }

  @Override
  public void invite(@NotNull OutputInfoVisitor visitor) {
    visitor.visit(this); 
  }

  @Override
  public int hashCode() {
    return myStyle;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FontStyle style = (FontStyle)o;

    return myStyle == style.myStyle;
  }

  @Override
  public String toString() {
    return "font style=" + myStyle;
  }
}
