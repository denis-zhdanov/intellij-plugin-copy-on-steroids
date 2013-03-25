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
}
