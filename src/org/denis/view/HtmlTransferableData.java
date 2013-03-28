package org.denis.view;

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

  private StringBuilder    myBuffer;
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
    myBuffer = holder;
    myRawText = rawText;
    myColorRegistry = syntaxInfo.getColorRegistry();
    myFontNameRegistry = syntaxInfo.getFontNameRegistry();
    try {
      myBuffer.append("<div style='border:1px inset;padding:2%;'>")
              .append("<pre style='margin:0;padding:6px;background-color:")
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
      myBuffer.append("'>"); 
      
      for (OutputInfo info : syntaxInfo.getOutputInfos()) {
        info.invite(this);
      }
      myBuffer.append("</pre>");
//      myBuffer.append("</pre></div>");
    }
    finally {
      myBuffer = null;
      myRawText = null;
      myColorRegistry = null;
      myFontNameRegistry = null;
      myIgnoreFontSize = false;
    }
  }
  
  @NotNull
  private String color(int id) {
    Color color = myColorRegistry.dataById(id);
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
  }

  private void appendFontFamilyRule(int fontFamilyId) {
    myBuffer.append("font-family:\"").append(myFontNameRegistry.dataById(fontFamilyId)).append("\";"); 
  }

  private void appendFontSizeRule(int fontSize) {
    myBuffer.append("font-size:").append(fontSize).append(';');
  }
  
  @Override
  public void visit(@NotNull Text text) {
    boolean formattedText = myForeground > 0 || myBackground > 0 || myFontFamily > 0 || myFontSize > 0 || myBold || myItalic;
    if (!formattedText) {
      escapeAndAdd(text.getStartOffset(), text.getEndOffset());
      return;
    }
    
    myBuffer.append("<span style='");
    if (myForeground > 0) {
      myBuffer.append("color:").append(color(myForeground)).append(";");
    }
    if (myBackground > 0) {
      myBuffer.append("background-color:").append(color(myBackground)).append(";");
    }
    if (myFontFamily > 0) {
      appendFontFamilyRule(myFontFamily);
    }
    if (myFontSize > 0) {
      appendFontSizeRule(myFontSize);
    }
    if (myBold) {
      myBuffer.append("font-weight:bold;");
    }
    if (myItalic) {
      myBuffer.append("font-style:italic;");
    }
    myBuffer.append("'>");
    escapeAndAdd(text.getStartOffset(), text.getEndOffset());
    myBuffer.append("</span>");
  }

  private void escapeAndAdd(int start, int end) {
    for (int i = start; i < end; i++) {
      char c = myRawText.charAt(i);
      switch (c) {
        case '<': myBuffer.append("&lt;"); break;
        case '>': myBuffer.append("&gt;"); break;
        case '&': myBuffer.append("&amp;"); break;
        case '\n': myBuffer.append("<br/>"); break;
        default: myBuffer.append(c);
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
