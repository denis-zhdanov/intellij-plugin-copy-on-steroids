package org.denis.view;

import org.denis.model.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;

public class RtfTransferableData extends AbstractSyntaxAwareInputStreamTransferableData {

  private static final long serialVersionUID = 1L;

  @NotNull private static final DataFlavor FLAVOR = new DataFlavor("text/rtf;class=java.io.InputStream", "RTF text");

  @NotNull private static final String HEADER_PREFIX = "{\\rtf1\\ansi\\deff0";
  @NotNull private static final String HEADER_SUFFIX = "}";
  @NotNull private static final String TAB           = "\\tab\n";
  @NotNull private static final String NEW_LINE      = "\\line\n";
  @NotNull private static final String BOLD          = "\\b";
  @NotNull private static final String ITALIC        = "\\i";
  @NotNull private static final String PLAIN         = "\\plain\n";

  public RtfTransferableData(@NotNull SyntaxInfo syntaxInfo) {
    super(syntaxInfo);
  }

  @Override
  public DataFlavor getFlavor() {
    return FLAVOR;
  }

  @Override
  protected void build(@NotNull final SyntaxInfo syntaxInfo, @NotNull final String rawText, @NotNull final StringBuilder holder) {
    header(syntaxInfo, holder, new Runnable() {
      @Override
      public void run() {
        rectangularBackground(syntaxInfo, holder, new Runnable() {
          @Override
          public void run() {
            content(syntaxInfo, holder, rawText);
          }
        });
      }
    });
  }

  private static void header(@NotNull SyntaxInfo syntaxInfo, @NotNull StringBuilder buffer, @NotNull Runnable next) {
    buffer.append(HEADER_PREFIX);

    // Color table.
    buffer.append("{\\colortbl;");
    ColorRegistry colorRegistry = syntaxInfo.getColorRegistry();
    for (int id : colorRegistry.getAllIds()) {
      Color color = colorRegistry.dataById(id);
      buffer.append(String.format("\\red%d\\green%d\\blue%d;", color.getRed(), color.getGreen(), color.getBlue()));
    }
    buffer.append("}\n");
    
    // Font table.
    buffer.append("{\\fonttbl");
    FontNameRegistry fontNameRegistry = syntaxInfo.getFontNameRegistry();
    for (int id : fontNameRegistry.getAllIds()) {
      String fontName = fontNameRegistry.dataById(id);
      buffer.append(String.format("{\\f%d %s;}", id, fontName));
    }
    buffer.append("}\n");

    next.run();
    buffer.append(HEADER_SUFFIX);
  }

  private static void rectangularBackground(@NotNull SyntaxInfo syntaxInfo, @NotNull StringBuilder buffer, @NotNull Runnable next) {
    buffer.append("\n\\s0\\box\\brdrhair\\brdrcf").append(syntaxInfo.getDefaultForeground()).append("\\brsp317").append("\\cbpat")
      .append(syntaxInfo.getDefaultBackground());
    saveBackground(buffer, syntaxInfo.getDefaultBackground());
    next.run();
    buffer.append("\\par");
  }

  private static void content(@NotNull SyntaxInfo syntaxInfo, @NotNull StringBuilder buffer, @NotNull String rawText) {
    MyVisitor visitor = new MyVisitor(buffer, rawText);
    for (OutputInfo info : syntaxInfo.getOutputInfos()) {
      info.invite(visitor);
    }
  }

  private static void saveBackground(@NotNull StringBuilder buffer, int id) {
    buffer.append("\\cb").append(id);
  }

  private static void saveForeground(@NotNull StringBuilder buffer, int id) {
    buffer.append("\\cf").append(id);
  }

  private static void saveFontName(@NotNull StringBuilder buffer, int id) {
    buffer.append("\\f").append(id);
  }

  private static void saveFontSize(@NotNull StringBuilder buffer, int size) {
    buffer.append("\\fs").append(size * 2);
  }

  private static class MyVisitor implements OutputInfoVisitor {

    @NotNull private final StringBuilder myBuffer;
    @NotNull private final String        myRawText;

    private int myBackgroundId = -1;
    private int myForegroundId = -1;
    private int myFontNameId   = -1;
    private int myFontStyle    = -1;
    private int myFontSize     = -1;

    MyVisitor(@NotNull StringBuilder buffer, @NotNull String rawText) {
      myBuffer = buffer;
      myRawText = rawText;
    }

    @Override
    public void visit(@NotNull Text text) {
      myBuffer.append("\n");
      for (int i = text.getStartOffset(), limit = text.getEndOffset(); i < limit; i++) {
        char c = myRawText.charAt(i);
        if (c > 127) {
          // Escape non-ascii symbols.
          myBuffer.append(String.format("\\u%04d?", (int)c));
          continue;
        }

        switch (c) {
          case '\t':
            myBuffer.append(TAB);
            continue;
          case '\n':
            myBuffer.append(NEW_LINE);
            continue;
          case '\\':
          case '{':
          case '}':
            myBuffer.append('\\');
        }
        myBuffer.append(c);
      }
    }

    @Override
    public void visit(@NotNull Foreground color) {
      saveForeground(myBuffer, color.getId());
      myForegroundId = color.getId();
    }

    @Override
    public void visit(@NotNull Background color) {
      saveBackground(myBuffer, color.getId());
      myBackgroundId = color.getId();
    }

    @Override
    public void visit(@NotNull FontFamilyName name) {
      saveFontName(myBuffer, name.getId());
      myFontNameId = name.getId();
    }

    @Override
    public void visit(@NotNull FontSize size) {
      saveFontSize(myBuffer, size.getSize());
      myFontSize = size.getSize();
    }

    @Override
    public void visit(@NotNull FontStyle style) {
      // Reset formatting settings
      myBuffer.append(PLAIN);

      // Restore target formatting settings.
      if (myForegroundId >= 0) {
        saveForeground(myBuffer, myForegroundId);
      }
      if (myBackgroundId >= 0) {
        saveBackground(myBuffer, myBackgroundId);
      }
      if (myFontNameId >= 0) {
        saveFontName(myBuffer, myFontNameId);
      }
      if (myFontSize > 0) {
        saveFontSize(myBuffer, myFontSize);
      }

      myFontStyle = style.getStyle();
      if ((myFontStyle & Font.ITALIC) > 0) {
        myBuffer.append(ITALIC);
      }
      if ((myFontStyle & Font.BOLD) > 0) {
        myBuffer.append(BOLD);
      }
    }
  }
}
