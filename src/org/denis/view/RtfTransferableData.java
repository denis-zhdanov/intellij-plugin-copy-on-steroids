package org.denis.view;

import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.util.StringBuilderSpinAllocator;
import org.denis.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class RtfTransferableData extends InputStream implements TextBlockTransferableData, Serializable {

  private static final long serialVersionUID = 1L;

  @NotNull private static final DataFlavor FLAVOR = new DataFlavor("text/rtf;class=java.io.InputStream", "RTF text");

  @NotNull private static final String HEADER_PREFIX  = "{\\rtf1\\ansi\\deff0";
  @NotNull private static final String HEADER_SUFFIX  = "}";
  @NotNull private static final String TAB            = "\\tab";
  @NotNull private static final String NEW_LINE       = "\\line\n";
  @NotNull private static final String BOLD           = "\\b";
  @NotNull private static final String ITALIC         = "\\i";
  @NotNull private static final String PLAIN          = "\\plain\n";

  @NotNull private final SyntaxInfo mySyntaxInfo;

  @Nullable private transient InputStream myDelegate;

  public RtfTransferableData(@NotNull SyntaxInfo syntaxInfo) {
    mySyntaxInfo = syntaxInfo;
  }

  @Override
  public DataFlavor getFlavor() {
    return FLAVOR;
  }

  @Override
  public int getOffsetCount() {
    return 0;
  }

  @Override
  public int getOffsets(int[] offsets, int index) {
    return index;
  }

  @Override
  public int setOffsets(int[] offsets, int index) {
    return index;
  }

  @Override
  public int read() throws IOException {
    return getDelegate().read();
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return getDelegate().read(b, off, len);
  }
  
  @Override
  public void close() throws IOException {
    myDelegate = null; 
  }

  @NotNull
  private InputStream getDelegate() {
    if (myDelegate != null) {
      return myDelegate;
    }
    Transferable contents = CopyPasteManager.getInstance().getContents();
    final String rawText;
    assert contents != null;
    try {
      rawText = (String)contents.getTransferData(DataFlavor.stringFlavor);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }

    final StringBuilder buffer = StringBuilderSpinAllocator.alloc();
    try {
      header(buffer, new Runnable() {
        @Override
        public void run() {
          rectangularBackground(buffer, new Runnable() {
            @Override
            public void run() {
              content(buffer, rawText);
            }
          }); 
        }
      });
      myDelegate = new ByteArrayInputStream(buffer.toString().getBytes());
      return myDelegate;
    }
    finally {
      StringBuilderSpinAllocator.dispose(buffer);
    }
  }

  private void header(@NotNull StringBuilder buffer, @NotNull Runnable next) {
    buffer.append(HEADER_PREFIX);

    // Color table.
    buffer.append("{\\colortbl;");
    ColorRegistry colorRegistry = mySyntaxInfo.getColorRegistry();
    for (int id : colorRegistry.getAllIds()) {
      Color color = colorRegistry.dataById(id);
      buffer.append(String.format("\\red%d\\green%d\\blue%d;", color.getRed(), color.getGreen(), color.getBlue()));
    }
    buffer.append("}\n");
    
    // Font table.
    buffer.append("{\\fonttbl");
    FontNameRegistry fontNameRegistry = mySyntaxInfo.getFontNameRegistry();
    for (int id : fontNameRegistry.getAllIds()) {
      String fontName = fontNameRegistry.dataById(id);
      buffer.append(String.format("{\\f%d %s;}", id, fontName));
    }
    buffer.append("}\n");

    next.run();
    buffer.append(HEADER_SUFFIX);
  }

  private void rectangularBackground(@NotNull StringBuilder buffer, @NotNull Runnable next) {
    buffer.append("\n\\s0\\box\\brdrhair\\brdrcf").append(mySyntaxInfo.getDefaultForeground()).append("\\brsp317");
    saveBackground(buffer, mySyntaxInfo.getDefaultBackground());
    next.run();
    buffer.append("\\par");
  }

  private void content(@NotNull StringBuilder buffer, @NotNull String rawText) {
    MyVisitor visitor = new MyVisitor(buffer, rawText);
    for (OutputInfo info : mySyntaxInfo.getOutputInfos()) {
      info.invite(visitor);
    }
  }

  private static void saveBackground(@NotNull StringBuilder buffer, int id) {
    buffer.append(String.format("\\cbpat%1$d\\cb%1$d", id));
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
