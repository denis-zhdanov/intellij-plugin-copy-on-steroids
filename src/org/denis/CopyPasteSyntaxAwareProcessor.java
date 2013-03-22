package org.denis;

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.impl.FontInfo;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.StringBuilderSpinAllocator;
import com.intellij.util.containers.ContainerUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.util.List;

public class CopyPasteSyntaxAwareProcessor implements CopyPastePostProcessor<SyntaxAwareTransferableData> {

  private static final String HEADER_PREFIX = "{\\rtf1\\ansi\\deff0";
  private static final String HEADER_SUFFIX = "\n}";
  private static final String TAB           = "\\tab";
  private static final String NEW_LINE      = "\\line\n";
  private static final String BOLD          = "\\b";
  private static final String ITALIC        = "\\i";
  private static final String PLAIN         = "\\plain\n";

  @Nullable
  @Override
  public SyntaxAwareTransferableData collectTransferableData(PsiFile file, Editor editor, int[] startOffsets, int[] endOffsets) {
    CharSequence text = editor.getDocument().getCharsSequence();
    EditorHighlighter highlighter = HighlighterFactory.createHighlighter(file.getProject(), file.getVirtualFile());
    highlighter.setText(text);
    Context context = new Context(editor);
    for (int i = 0; i < startOffsets.length; i++) {
      int startOffset = startOffsets[i];
      int endOffset = endOffsets[i];
      HighlighterIterator iterator = highlighter.createIterator(startOffset);
      context.onNewSelectionLine();
      while (!iterator.atEnd()) {
        int tokenStart = Math.max(iterator.getStart(), startOffset);
        if (tokenStart >= endOffset) {
          break;
        }
        int tokenEnd = Math.min(endOffset, iterator.getEnd());
        TextAttributes attributes = iterator.getTextAttributes();

        if (attributes == null) {
          continue;
        }
        
        context.processToken(text, tokenStart, tokenEnd, attributes);
        iterator.advance();
      }
      context.buffer.append(NEW_LINE);
    }
    if (context.buffer.length() <= 0) {
      return null;
    }
    context.buffer.delete(context.buffer.length() - NEW_LINE.length(), context.buffer.length());
    return new SyntaxAwareTransferableData(context.finish());
  }

  @Nullable
  @Override
  public SyntaxAwareTransferableData extractTransferableData(Transferable content) {
    return null;
  }

  @Override
  public void processTransferableData(Project project,
                                      Editor editor,
                                      RangeMarker bounds,
                                      int caretOffset,
                                      Ref<Boolean> indented,
                                      SyntaxAwareTransferableData value)
  {
  }
  
  private static class Context {

    @NotNull final StringBuilder buffer = StringBuilderSpinAllocator.alloc();

    @NotNull private final ColorTable myColorTable;
    @NotNull private final FontTable  myFontTable;
    @NotNull private final Editor     myEditor;
    @NotNull private final Color      myDefaultForegroundColor;
    @NotNull private final Color      myDefaultBackgroundColor;

    @Nullable private Color  myPrevForegroundColor;
    @Nullable private Color  myPrevBackgroundColor;
    @Nullable private String myPrevFontName;

    private int myPrevFontType;
    private int myPrevFontSize;

    Context(@NotNull Editor editor) {
      myEditor = editor;
      EditorColorsScheme colorsScheme = editor.getColorsScheme();
      myDefaultForegroundColor = colorsScheme.getDefaultForeground();
      myDefaultBackgroundColor = colorsScheme.getDefaultBackground();
      myColorTable = new ColorTable();
      myFontTable = new FontTable();
    }

    void onNewSelectionLine() {
      myPrevForegroundColor = null;
      myPrevBackgroundColor = null;
      myPrevFontType = Font.PLAIN;
      myPrevFontSize = -1;
    }

    void processToken(@NotNull CharSequence text, int start, int end, @NotNull TextAttributes attributes) {
      if (!containsOnlyWhiteSpaces(text, start, end)) {
        processFontType(attributes);
        processForeground(attributes);
        processBackground(attributes);
      }
      append(text, start, end);
    }

    private static boolean containsOnlyWhiteSpaces(@NotNull CharSequence text, int start, int end) {
      for (int i = start; i < end; i++) {
        char c = text.charAt(i);
        if (c != ' ' && c != '\t' && c != '\n') {
          return false;
        }
      }
      return true;
    }

    private void processForeground(@NotNull TextAttributes attributes) {
      Color foregroundColor = attributes.getForegroundColor();
      if (foregroundColor == null) {
        foregroundColor = myDefaultForegroundColor;
      }
      if (myPrevForegroundColor == null || !myPrevForegroundColor.equals(foregroundColor)) {
        appendFormatRule(myColorTable.getCode(foregroundColor, true));
        myPrevForegroundColor = foregroundColor;
      }
    }

    private void processBackground(@NotNull TextAttributes attributes) {
      Color backgroundColor = attributes.getBackgroundColor();
      if (backgroundColor == null) {
        backgroundColor = myDefaultBackgroundColor;
      }
      if (myPrevBackgroundColor == null || !myPrevBackgroundColor.equals(backgroundColor)) {
        appendFormatRule(myColorTable.getCode(backgroundColor, false));
        myPrevBackgroundColor = backgroundColor;
      }
    }

    private void processFontType(@NotNull TextAttributes attributes) {
      int fontType = attributes.getFontType();
      if (myPrevFontType == fontType) {
        return;
      }

      if (fontType == Font.PLAIN) {
        appendFormatRule(PLAIN);
        myPrevFontSize = -1;
        myPrevFontName = null;
        myPrevFontType = Font.PLAIN;
        return;
      }

      if (myPrevFontType > 0) {
        // Need to reset font type.
        appendFormatRule(PLAIN);
        // PLAIN mark resets font size as well, so, re-install it manually.
        if (myPrevFontSize > 0) {
          appendFormatRule(FontTable.getSizeCode(myPrevFontSize));
        }
        if (myPrevFontName != null) {
          appendFormatRule(myFontTable.getNameCode(myPrevFontName));
        }
      }
      myPrevFontType = fontType;
      if ((fontType & Font.ITALIC) > 0) {
        appendFormatRule(ITALIC);
      }
      if ((fontType & Font.BOLD) > 0) {
        appendFormatRule(BOLD);
      }
    }

    private void append(@NotNull CharSequence text, int start, int end) {
      for (int i = StringUtil.indexOf(text, '\n', start);
           start < end && start < text.length() && i >= 0 && i < end;
           i = StringUtil.indexOf(text, '\n', start))
      {
        escapeAndAppend(text, start, i);
        buffer.append(NEW_LINE);
        start = i + 1;
      }
      if (start < text.length()) {
        escapeAndAppend(text, start, end);
      }
    }

    private void escapeAndAppend(@NotNull CharSequence text, int start, int end) {
      for (int i = start; i < end; i++) {
        char c = text.charAt(i);
        switch (c) {
          case '\t': buffer.append(TAB); continue;
          case ' ': buffer.append(' '); continue;
          case '{':
          case '}':
            buffer.append('\\');
        }
        
        FontInfo fontInfo = EditorUtil.fontForChar(c, myPrevFontType, myEditor);
        String currentFontName = fontInfo.getFont().getName();
        if (myPrevFontName == null || !myPrevFontName.equals(currentFontName)) {
          appendFormatRule(myFontTable.getNameCode(currentFontName));
          myPrevFontName = currentFontName;
        }
        
        int currentFontSize = fontInfo.getSize();
        if (myPrevFontSize < 0 || myPrevFontSize != currentFontSize) {
          appendFormatRule(FontTable.getSizeCode(currentFontSize));
          myPrevFontSize = currentFontSize;
        }

        if (c > 127) {
          buffer.append(String.format("\\u%04d?", (int)c));
        }
        else {
          buffer.append(c);
        }
      }
    }

    private void appendFormatRule(@NotNull String ruleText) {
      // There is a possible case that one rule follows another one, e.g. bold italic text.
      // We don't want to produce text like '\n\\b\n\n\\i\n' then (wrap every rule into line feed symbols).
      // Desired result is '\n\\b\n\\i\n'.
      if (buffer.length() <= 0 || buffer.charAt(buffer.length() - 1) != '\n') {
        buffer.append('\n');
      }
      buffer.append(ruleText).append('\n');
    }

    String finish() {
      buffer.insert(0, myFontTable.getTableDescription());
      buffer.insert(0, myColorTable.getTableDescription());
      buffer.insert(0, HEADER_PREFIX);
      buffer.append(HEADER_SUFFIX);
      return buffer.toString();
    }
  }

  private static class ColorTable {

    @NotNull private final List<Color> myColors = ContainerUtilRt.newArrayList();

    @NotNull
    public String getCode(@NotNull Color color, boolean foreground) {
      int i = myColors.indexOf(color);
      if (i < 0) {
        myColors.add(color);
        i = myColors.size() - 1;
      }
      return String.format("\\c%c%d", foreground ? 'f' : 'b', i + 1);
    }

    @NotNull
    public String getTableDescription() {
      StringBuilder buffer = new StringBuilder("\n{\\colortbl;");
      for (Color color : myColors) {
        buffer.append(String.format("\\red%d\\green%d\\blue%d;", color.getRed(), color.getGreen(), color.getBlue()));
      }
      buffer.append("}");
      return buffer.toString();
    }
  }
  
  private static class FontTable {
    
    @NotNull private final List<String> myFontNames = ContainerUtilRt.newArrayList();
    
    @NotNull
    public String getNameCode(@NotNull String fontName) {
      int i = myFontNames.indexOf(fontName);
      if (i < 0) {
        myFontNames.add(fontName);
        i = myFontNames.size() - 1;
      }
      return String.format("\\f%d", i);
    }

    @NotNull
    public static String getSizeCode(int size) {
      return String.format("\\fs%s", size * 2);
    }

    @NotNull
    public String getTableDescription() {
      StringBuilder buffer = new StringBuilder("\n{\\fonttbl");
      int i = -1;
      for (String fontName : myFontNames) {
        buffer.append(String.format("{\\f%d %s;}", ++i, fontName));
      }
      buffer.append("}");
      return buffer.toString();
    }
  }
}
