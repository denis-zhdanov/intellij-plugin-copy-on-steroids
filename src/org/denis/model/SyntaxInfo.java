package org.denis.model;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 3/25/13 1:19 PM
 */
public class SyntaxInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  @NotNull private final List<OutputInfo> myOutputInfos;
  @NotNull private final ColorRegistry    myColorRegistry;
  @NotNull private final FontNameRegistry myFontNameRegistry;

  private final int myDefaultForeground;
  private final int myDefaultBackground;

  public SyntaxInfo(@NotNull List<OutputInfo> infos,
                    int defaultForeground,
                    int defaultBackground,
                    @NotNull FontNameRegistry fontNameRegistry,
                    @NotNull ColorRegistry colorRegistry)
  {
    myOutputInfos = infos;
    myDefaultForeground = defaultForeground;
    myDefaultBackground = defaultBackground;
    myFontNameRegistry = fontNameRegistry;
    myColorRegistry = colorRegistry;
  }

  @NotNull
  public List<OutputInfo> getOutputInfos() {
    return myOutputInfos;
  }

  @NotNull
  public ColorRegistry getColorRegistry() {
    return myColorRegistry;
  }

  @NotNull
  public FontNameRegistry getFontNameRegistry() {
    return myFontNameRegistry;
  }

  public int getDefaultForeground() {
    return myDefaultForeground;
  }

  public int getDefaultBackground() {
    return myDefaultBackground;
  }

  @Override
  public String toString() {
    return String.format("default colors: foreground=%d, background=%d; output infos: %s",
                         myDefaultForeground, myDefaultBackground, myOutputInfos);
  }
}
