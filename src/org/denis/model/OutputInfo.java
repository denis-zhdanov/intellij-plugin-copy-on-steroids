package org.denis.model;

import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 * @since 3/23/13 3:06 PM
 */
public interface OutputInfo {

  void invite(@NotNull OutputInfoVisitor visitor);
}
