package org.denis.model;

import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 * @since 3/23/13 3:07 PM
 */
public interface OutputInfoVisitor {

  void visit(@NotNull Text text);
  void visit(@NotNull Foreground color);
  void visit(@NotNull Background color);
  void visit(@NotNull FontFamilyName name);
  void visit(@NotNull FontStyle style);
  void visit(@NotNull FontSize size);
}
