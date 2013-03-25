package org.denis;

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor;
import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.DisposableIterator;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import org.denis.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.util.*;
import java.util.List;
import java.util.Queue;

public abstract class AbstractCopyPasteSyntaxAwareProcessor<T extends TextBlockTransferableData> implements CopyPastePostProcessor<T> {

  @SuppressWarnings("unchecked")
  @Nullable
  @Override
  public T collectTransferableData(PsiFile file, Editor editor, int[] startOffsets, int[] endOffsets) {
    CharSequence text = editor.getDocument().getCharsSequence();
    EditorHighlighter highlighter = HighlighterFactory.createHighlighter(file.getProject(), file.getVirtualFile());
    highlighter.setText(text);
    MarkupModel markupModel = DocumentMarkupModel.forDocument(editor.getDocument(), file.getProject(), false);
    Context context = new Context(editor);
    int shift = 0;
    int prevEndOffset = 0;
    
    for (int i = 0; i < startOffsets.length; i++) {
      shift += prevEndOffset - startOffsets[i];
      prevEndOffset = endOffsets[i];
      context.reset(shift);
      Iterator<SegmentInfo> it = aggregateSyntaxInfo(wrap(highlighter, editor, startOffsets[i], endOffsets[i]),
                                                     wrap(markupModel, editor, startOffsets[i], endOffsets[i]));
      while (it.hasNext()) {
        SegmentInfo info = it.next();
        if (info.startOffset >= endOffsets[i]) {
          break;
        }
        context.onNewData(info);
      }
      context.onIterationEnd(endOffsets[i]);
    }
    return build(context.finish());
  }

  @Nullable
  protected abstract T build(@NotNull SyntaxInfo info);

  private static Iterator<SegmentInfo> aggregateSyntaxInfo(@NotNull final Iterator<List<SegmentInfo>>... iterators) {
    return new Iterator<SegmentInfo>() {

      @NotNull private final Queue<SegmentInfo> myInfos = new PriorityQueue<SegmentInfo>();
      @NotNull private final Map<SegmentInfo, Iterator<List<SegmentInfo>>> myEndMarkers = ContainerUtilRt.newHashMap();

      {
        for (Iterator<List<SegmentInfo>> iterator : iterators) {
          extract(iterator);
        }
      }

      @Override
      public boolean hasNext() {
        return !myInfos.isEmpty();
      }

      @Override
      public SegmentInfo next() {
        SegmentInfo result = myInfos.remove();
        Iterator<List<SegmentInfo>> iterator = myEndMarkers.get(result);
        if (iterator != null) {
          extract(iterator);
        }
        return result;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      private void extract(@NotNull Iterator<List<SegmentInfo>> iterator) {
        while (iterator.hasNext()) {
          List<SegmentInfo> infos = iterator.next();
          if (infos.isEmpty()) {
            continue;
          }
          myInfos.addAll(infos);
          myEndMarkers.put(infos.get(infos.size() - 1), iterator);
          break;
        }
      }
    };
  }
  
  @NotNull
  private static Iterator<List<SegmentInfo>> wrap(@NotNull final EditorHighlighter highlighter,
                                                  @NotNull final Editor editor,
                                                  final int startOffset,
                                                  final int endOffset)
  {
    final HighlighterIterator highlighterIterator = highlighter.createIterator(startOffset);
    return new Iterator<List<SegmentInfo>>() {
      
      @Nullable private List<SegmentInfo> myCached;
      
      @Override
      public boolean hasNext() {
        return myCached != null || updateCached();
      }

      @Override
      public List<SegmentInfo> next() {
        if (myCached != null) {
          List<SegmentInfo> result = myCached;
          myCached = null;
          return result;
        }

        if (!updateCached()) {
          throw new UnsupportedOperationException();
        }
        return myCached;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      private boolean updateCached() {
        if (highlighterIterator.atEnd()) {
          return false;
        }
        int tokenStart = Math.max(highlighterIterator.getStart(), startOffset);
        if (tokenStart >= endOffset) {
          return false;
        }
        
        TextAttributes attributes = highlighterIterator.getTextAttributes();
        int tokenEnd = Math.min(highlighterIterator.getEnd(), endOffset);
        myCached = SegmentInfo.produce(attributes, editor, tokenStart, tokenEnd);
        highlighterIterator.advance();
        return true;
      }
    };
  }

  @NotNull
  private static Iterator<List<SegmentInfo>> wrap(@NotNull MarkupModel model,
                                                  @NotNull final Editor editor,
                                                  final int startOffset,
                                                  final int endOffset)
  {
    if (!(model instanceof MarkupModelEx)) {
      return ContainerUtil.emptyIterator();
    }
    final DisposableIterator<RangeHighlighterEx> iterator = ((MarkupModelEx)model).overlappingIterator(startOffset, endOffset);
    return new Iterator<List<SegmentInfo>>() {

      @Nullable private List<SegmentInfo> myCached;
      
      @Override
      public boolean hasNext() {
        return myCached != null || iterator.hasNext();
      }

      @Override
      public List<SegmentInfo> next() {
        if (myCached != null) {
          List<SegmentInfo> result = myCached;
          myCached = null;
          return result;
        }

        if (!updateCached()) {
          throw new UnsupportedOperationException();
        }
        return myCached;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      private boolean updateCached() {
        if (!iterator.hasNext()) {
          return false;
        }
        
        RangeHighlighterEx highlighter = iterator.next();
        while (highlighter == null || !highlighter.isValid() || highlighter.getTextAttributes() == null) {
          if (!iterator.hasNext()) {
            return false;
          }
          highlighter = iterator.next();
        }
        
        int tokenStart = Math.max(highlighter.getStartOffset(), startOffset);
        if (tokenStart >= endOffset) {
          return false;
        }

        TextAttributes attributes = highlighter.getTextAttributes();
        int tokenEnd = Math.min(highlighter.getEndOffset(), endOffset);
        //noinspection ConstantConditions
        myCached = SegmentInfo.produce(attributes, editor, tokenStart, tokenEnd);
        return true;
      }
    };
  }

  @Nullable
  @Override
  public T extractTransferableData(Transferable content) {
    return null;
  }

  @Override
  public void processTransferableData(Project project,
                                      Editor editor,
                                      RangeMarker bounds,
                                      int caretOffset,
                                      Ref<Boolean> indented,
                                      T value)
  {
  }
  
  private static class Context {

    @NotNull List<OutputInfo> myOutputInfos = ContainerUtilRt.newArrayList();

    @NotNull private final ColorRegistry    myColorRegistry    = new ColorRegistry();
    @NotNull private final FontNameRegistry myFontNameRegistry = new FontNameRegistry();

    @NotNull private final Color myDefaultForeground;
    @NotNull private final Color myDefaultBackground;

    @Nullable private Color  myBackground;
    @Nullable private Color  myForeground;
    @Nullable private String myFontFamilyName;

    private int myFontStyle;
    private int myStartOffset;
    private int myOffsetShift;

    Context(@NotNull Editor editor) {
      EditorColorsScheme colorsScheme = editor.getColorsScheme();
      myDefaultForeground = colorsScheme.getDefaultForeground();
      myDefaultBackground = colorsScheme.getDefaultBackground();
    }

    public void reset(int offsetShift) {
      myBackground = null;
      myForeground = null;
      myFontFamilyName = null;
      myFontStyle = Font.PLAIN;
      myStartOffset = -1;
      myOffsetShift = offsetShift;
    }

    public void onNewData(@NotNull SegmentInfo info) {
      if (myStartOffset < 0) {
        onFirstSegment(info);
        return;
      }

      processBackground(info);
      processForeground(info);
      processFontFamilyName(info);
      processFontStyle(info);

      // TODO den add font size support.
    }

    private void processFontStyle(@NotNull SegmentInfo info) {
      if (info.fontStyle != myFontStyle) {
        addTextIfPossible(info.startOffset);
        myOutputInfos.add(new FontStyle(myFontStyle));
      }
    }

    private void processFontFamilyName(@NotNull SegmentInfo info) {
      if (!info.fontFamilyName.equals(myFontFamilyName)) {
        addTextIfPossible(info.startOffset);
        if (myFontFamilyName != null) {
          myOutputInfos.add(new FontFamilyName(myFontNameRegistry.getId(myFontFamilyName)));
        }
        myFontFamilyName = info.fontFamilyName;
      }
    }

    private void processForeground(@NotNull SegmentInfo info) {
      if (myForeground == null && info.foreground != null) {
        addTextIfPossible(info.startOffset);
        myForeground = info.foreground;
        myOutputInfos.add(new Foreground(myColorRegistry.getId(info.foreground)));
      }
      else if (myForeground != null) {
        Color c = info.foreground == null ? myDefaultForeground : info.foreground;
        if (!myForeground.equals(c)) {
          addTextIfPossible(info.startOffset);
          myOutputInfos.add(new Foreground(myColorRegistry.getId(myForeground)));
          myForeground = c;
        }
      }
    }

    private void processBackground(@NotNull SegmentInfo info) {
      if (myBackground == null && info.background != null) {
        addTextIfPossible(info.startOffset);
        myBackground = info.background;
        myOutputInfos.add(new Background(myColorRegistry.getId(info.background)));
      }
      else if (myBackground != null) {
        Color c = info.background == null ? myDefaultBackground : info.background;
        if (!myBackground.equals(c)) {
          addTextIfPossible(info.startOffset);
          myOutputInfos.add(new Background(myColorRegistry.getId(myBackground)));
          myBackground = c;
        }
      }
    }

    private void onFirstSegment(@NotNull SegmentInfo info) {
      myStartOffset = info.startOffset;

      myFontStyle = info.fontStyle;
      if (myFontStyle != Font.PLAIN) {
        myOutputInfos.add(new FontStyle(myFontStyle));
      }

      myBackground = info.background;
      if (myBackground != null) {
        myOutputInfos.add(new Background(myColorRegistry.getId(myBackground)));
      }

      myForeground = info.foreground;
      if (myForeground != null) {
        myOutputInfos.add(new Foreground(myColorRegistry.getId(myForeground)));
      }

      myFontFamilyName = info.fontFamilyName;
      myOutputInfos.add(new FontFamilyName(myFontNameRegistry.getId(myFontFamilyName)));
    }

    private void addTextIfPossible(int endOffset) {
      if (endOffset > myStartOffset) {
        myOutputInfos.add(new Text(myStartOffset + myOffsetShift, endOffset + myOffsetShift));
        myStartOffset = endOffset;
      }
    }

    @NotNull
    public List<OutputInfo> onIterationEnd(int endOffset) {
      if (myStartOffset >= 0 && myStartOffset < endOffset) {
        if (myForeground != null) {
          myOutputInfos.add(new Foreground(myColorRegistry.getId(myForeground)));
        }
        if (myBackground != null) {
          myOutputInfos.add(new Background(myColorRegistry.getId(myBackground)));
        }
        if (myFontFamilyName != null) {
          myOutputInfos.add(new FontFamilyName(myFontNameRegistry.getId(myFontFamilyName)));
        }
        myOutputInfos.add(new Text(myStartOffset + myOffsetShift, endOffset + myOffsetShift));
      }
      return myOutputInfos;
    }

    @NotNull
    public SyntaxInfo finish() {
      // TODO den uncomment
//      myColorRegistry.seal();
//      myFontNameRegistry.seal();
      return new SyntaxInfo(myOutputInfos, myColorRegistry.getId(myDefaultBackground), myFontNameRegistry, myColorRegistry);
    }
  }

  private static class SegmentInfo implements Comparable<SegmentInfo> {

    @Nullable public final Color  foreground;
    @Nullable public final Color  background;
    @NotNull public final  String fontFamilyName;

    public final int fontStyle;
    public final int startOffset;
    public final int endOffset;

    SegmentInfo(@Nullable Color foreground,
                @Nullable Color background,
                @NotNull String fontFamilyName,
                int fontStyle,
                int startOffset,
                int endOffset)
    {
      this.foreground = foreground;
      this.background = background;
      this.fontFamilyName = fontFamilyName;
      this.fontStyle = fontStyle;
      this.startOffset = startOffset;
      this.endOffset = endOffset;
    }

    @NotNull
    public static List<SegmentInfo> produce(@NotNull TextAttributes attribute, @NotNull Editor editor, int start, int end) {
      if (end <= start) {
        return Collections.emptyList();
      }
      List<SegmentInfo> result = ContainerUtilRt.newArrayList();
      CharSequence text = editor.getDocument().getCharsSequence();
      int currentStart = start;
      String fontFamilyName = EditorUtil.fontForChar(text.charAt(start), attribute.getFontType(), editor).getFont().getFamily();
      String candidateFontFamilyName;
      for (int i = start + 1; i < end; i++) {
        candidateFontFamilyName = EditorUtil.fontForChar(text.charAt(i), attribute.getFontType(), editor).getFont().getFamily();
        if (!candidateFontFamilyName.equals(fontFamilyName)) {
          result.add(new SegmentInfo(
            attribute.getForegroundColor(), attribute.getBackgroundColor(), fontFamilyName, attribute.getFontType(), currentStart, i
          ));
          currentStart = i;
          fontFamilyName = candidateFontFamilyName;
        }
      }

      if (currentStart < end) {
        result.add(new SegmentInfo(
          attribute.getForegroundColor(), attribute.getBackgroundColor(), fontFamilyName, attribute.getFontType(), currentStart, end
        ));
      }

      return result;
    }

    @Override
    public int compareTo(SegmentInfo o) {
      return startOffset - o.startOffset;
    }

    @Override
    public int hashCode() {
      int result = foreground != null ? foreground.hashCode() : 0;
      result = 31 * result + (background != null ? background.hashCode() : 0);
      result = 31 * result + fontFamilyName.hashCode();
      result = 31 * result + fontStyle;
      result = 31 * result + startOffset;
      result = 31 * result + endOffset;
      return result;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      SegmentInfo info = (SegmentInfo)o;

      if (endOffset != info.endOffset) return false;
      if (fontStyle != info.fontStyle) return false;
      if (startOffset != info.startOffset) return false;
      if (background != null ? !background.equals(info.background) : info.background != null) return false;
      if (!fontFamilyName.equals(info.fontFamilyName)) return false;
      if (foreground != null ? !foreground.equals(info.foreground) : info.foreground != null) return false;

      return true;
    }

    @Override
    public String toString() {
      StringBuilder fontStyleAsString = new StringBuilder();
      if (fontStyle == Font.PLAIN) {
        fontStyleAsString.append("plain");
      }
      else {
        if ((fontStyle & Font.BOLD) != 0) {
          fontStyleAsString.append("bold ");
        }
        if ((fontStyle & Font.ITALIC) != 0) {
          fontStyleAsString.append("italic ");
        }
        if (fontStyleAsString.length() > 0) {
          fontStyleAsString.setLength(fontStyleAsString.length() - 1);
        }
        else {
          fontStyleAsString.append("unknown font style");
        }
      }
      return String.format("%d-%d: %s, %s", startOffset, endOffset, fontFamilyName, fontStyleAsString);
    }
  }
}
