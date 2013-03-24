package org.denis.model;

import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 * @since 3/23/13 3:23 PM
 */
public class Background extends AbstractColorInfo {

  public Background(@NotNull ColorRegistry registry, int id) {
    super(registry, id);
  }

  @Override
  public void invite(@NotNull OutputInfoVisitor visitor) {
    visitor.visit(this);
  }
}
