package org.denis.model;

import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 * @since 3/23/13 3:08 PM
 */
public class Text implements OutputInfo {
  
  private final int myStartOffset;
  private final int myEndOffset;

  public Text(int startOffset, int endOffset) {
    myStartOffset = startOffset;
    myEndOffset = endOffset;
  }

  @Override
  public void invite(@NotNull OutputInfoVisitor visitor) {
    visitor.visit(this);
  }

  public int getStartOffset() {
    return myStartOffset;
  }

  public int getEndOffset() {
    return myEndOffset;
  }

  @Override
  public int hashCode() {
    int result = myStartOffset;
    result = 31 * result + myEndOffset;
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Text text = (Text)o;
    return myEndOffset == text.myEndOffset && myStartOffset == text.myStartOffset;
  }

  @Override
  public String toString() {
    return String.format("text=%d-%d", myStartOffset, myEndOffset);
  }
}
