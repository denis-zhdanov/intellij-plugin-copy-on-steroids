package org.denis.view;

import com.intellij.util.StringBuilderSpinAllocator;
import org.denis.model.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;

/**
 * @author Denis Zhdanov
 * @since 3/28/13 1:06 PM
 */
public class HtmlTransferableData extends AbstractSyntaxAwareReaderTransferableData implements OutputInfoVisitor {

  @NotNull private static final DataFlavor FLAVOR = new DataFlavor("text/html;class=java.io.Reader", "HTML text");

  private static final long serialVersionUID = 1L;

  private StringBuilder    myResultBuffer;
  private String           myRawText;
  private ColorRegistry    myColorRegistry;
  private FontNameRegistry myFontNameRegistry;

  private int     myForeground;
  private int     myBackground;
  private int     myFontFamily;
  private int     myFontSize;
  private boolean myBold;
  private boolean myItalic;
  private boolean myIgnoreFontSize;

  public HtmlTransferableData(@NotNull SyntaxInfo syntaxInfo) {
    super(syntaxInfo);
  }

  @Override
  public DataFlavor getFlavor() {
    return FLAVOR;
  }

  @Override
  protected void build(@NotNull SyntaxInfo syntaxInfo, @NotNull String rawText, @NotNull StringBuilder holder) {
    myResultBuffer = holder;
    myRawText = rawText;
    myColorRegistry = syntaxInfo.getColorRegistry();
    myFontNameRegistry = syntaxInfo.getFontNameRegistry();
    try {
      myResultBuffer.append("<div style=\"border:1px inset;padding:2%;\">")
              .append("<pre style=\"margin:0;padding:6px;background-color:")
//              .append("<pre style='height:30%;overflow:auto;margin:0;padding:6px;background-color:")
              .append(color(syntaxInfo.getDefaultBackground())).append(';');
      if (myFontNameRegistry.size() == 1) {
        appendFontFamilyRule(myFontNameRegistry.getAllIds()[0]);
        myFontNameRegistry = null;
      }
      int fontSize = syntaxInfo.getSingleFontSize();
      if (fontSize > 0) {
        appendFontSizeRule(fontSize);
        myIgnoreFontSize = true;
      }
      myResultBuffer.append("\" bgcolor=\"").append(color(syntaxInfo.getDefaultBackground())).append("\">");
      
      for (OutputInfo info : syntaxInfo.getOutputInfos()) {
        info.invite(this);
      }
      myResultBuffer.append("</pre></div>");
    }
    finally {
      myResultBuffer = null;
      myRawText = null;
      myColorRegistry = null;
      myFontNameRegistry = null;
      myIgnoreFontSize = false;
    }
  }

  private void defineForeground(int id, @NotNull StringBuilder styleBuffer, @NotNull StringBuilder closeTagBuffer) {
    myResultBuffer.append("<font color=\"").append(color(id)).append("\">");
    styleBuffer.append("color:").append(color(id)).append(";");
    closeTagBuffer.insert(0, "</font>");
  }

  private void defineBold(@NotNull StringBuilder styleBuffer, @NotNull StringBuilder closeTagBuffer) {
    myResultBuffer.append("<b>");
    styleBuffer.append("font-weight:bold;");
    closeTagBuffer.insert(0, "</b>");
  }

  private void defineItalic(@NotNull StringBuilder styleBuffer, @NotNull StringBuilder closeTagBuffer) {
    myResultBuffer.append("<i>");
    styleBuffer.append("font-style:italic;");
    closeTagBuffer.insert(0, "</i>");
  }
  
  @NotNull
  private String color(int id) {
    Color color = myColorRegistry.dataById(id);
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
  }

  private void appendFontFamilyRule(int fontFamilyId) {
    myResultBuffer.append("font-family:'").append(myFontNameRegistry.dataById(fontFamilyId)).append("';"); 
  }

  private void appendFontSizeRule(int fontSize) {
    myResultBuffer.append("font-size:").append(fontSize).append(';');
  }

  @Override
  public void visit(@NotNull Text text) {
    boolean formattedText = myForeground > 0 || myBackground > 0 || myFontFamily > 0 || myFontSize > 0 || myBold || myItalic;
    if (!formattedText) {
      escapeAndAdd(text.getStartOffset(), text.getEndOffset());
      return;
    }

    StringBuilder styleBuffer = StringBuilderSpinAllocator.alloc();
    StringBuilder closeTagBuffer = StringBuilderSpinAllocator.alloc();
    try {
      if (myForeground > 0) {
        defineForeground(myForeground, styleBuffer, closeTagBuffer);
      }
      if (myBackground > 0) {
        myResultBuffer.append("background-color:").append(color(myBackground)).append(";");
      }
      if (myBold) {
        defineBold(styleBuffer, closeTagBuffer);
      }
      if (myItalic) {
        defineItalic(styleBuffer, closeTagBuffer);
      }
      if (myFontFamily > 0) {
        appendFontFamilyRule(myFontFamily);
      }
      if (myFontSize > 0) {
        appendFontSizeRule(myFontSize);
      }
      myResultBuffer.append("<span style=\"");
      myResultBuffer.append(styleBuffer);
      myResultBuffer.append("\">");
      escapeAndAdd(text.getStartOffset(), text.getEndOffset());
      myResultBuffer.append("</span>");
      myResultBuffer.append(closeTagBuffer);
    }
    finally {
      StringBuilderSpinAllocator.dispose(styleBuffer);
      StringBuilderSpinAllocator.dispose(closeTagBuffer);
    }
  }

  private void escapeAndAdd(int start, int end) {
    for (int i = start; i < end; i++) {
      char c = myRawText.charAt(i);
      switch (c) {
        case '<': myResultBuffer.append("&lt;"); break;
        case '>': myResultBuffer.append("&gt;"); break;
        case '&': myResultBuffer.append("&amp;"); break;
        default: myResultBuffer.append(c);
      }
    }
  }

  @Override
  public void visit(@NotNull Foreground color) {
    myForeground = color.getId();
  }

  @Override
  public void visit(@NotNull Background color) {
    myBackground = color.getId(); 
  }

  @Override
  public void visit(@NotNull FontFamilyName name) {
    if (myFontNameRegistry != null) {
      myFontFamily = name.getId();
    }
  }

  @Override
  public void visit(@NotNull FontStyle style) {
    myBold = (Font.BOLD & style.getStyle()) != 0;
    myItalic = (Font.ITALIC & style.getStyle()) != 0;
  }

  @Override
  public void visit(@NotNull FontSize size) {
    if (!myIgnoreFontSize) {
      myFontSize = size.getSize();
    }
  }
}
