package org.denis.model;

import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 * @since 3/23/13 3:11 PM
 */
public abstract class AbstractColorInfo implements OutputInfo {
  
  @NotNull private final ColorRegistry myRegistry;
  
  private final int myId;

  public AbstractColorInfo(@NotNull ColorRegistry registry, int id) {
    myRegistry = registry;
    myId = id;
  }

  @NotNull
  public ColorRegistry getRegistry() {
    return myRegistry;
  }

  public int getId() {
    return myId;
  }
}
