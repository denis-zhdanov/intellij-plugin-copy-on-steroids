package org.denis;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
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
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtilRt;
import org.denis.model.*;
import org.denis.settings.CopyOnSteroidSettings;
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
    EditorColorsScheme schemeToUse = CopyOnSteroidSettings.getInstance().getColorsScheme(editor);
    highlighter.setColorScheme(schemeToUse);
    MarkupModel markupModel = DocumentMarkupModel.forDocument(editor.getDocument(), file.getProject(), false);
    Context context = new Context(editor, schemeToUse);
    int shift = 0;
    int prevEndOffset = 0;
    
    for (int i = 0; i < startOffsets.length; i++) {
      shift += prevEndOffset - startOffsets[i];
      prevEndOffset = endOffsets[i];
      context.reset(shift);
      DisposableIterator<SegmentInfo> it = aggregateSyntaxInfo(editor,
                                                               wrap(highlighter, editor, startOffsets[i], endOffsets[i]),
                                                               wrap(markupModel, editor, schemeToUse, startOffsets[i], endOffsets[i]));
      try {
        while (it.hasNext()) {
          SegmentInfo info = it.next();
          if (info.startOffset >= endOffsets[i]) {
            break;
          }
          context.onNewData(info);
        }
      }
      finally {
        it.dispose();
      }
      context.onIterationEnd(endOffsets[i]);
    }
    return build(context.finish());
  }

  @Nullable
  protected abstract T build(@NotNull SyntaxInfo info);

  private static DisposableIterator<SegmentInfo> aggregateSyntaxInfo(@NotNull Editor editor,
                                                                     @NotNull final DisposableIterator<List<SegmentInfo>>... iterators)
  {
    EditorColorsScheme colorsScheme = editor.getColorsScheme();
    final Color defaultForeground = colorsScheme.getDefaultForeground();
    final Color defaultBackground = colorsScheme.getDefaultBackground();
    return new DisposableIterator<SegmentInfo>() {

      @NotNull private final Queue<SegmentInfo> myInfos = new PriorityQueue<SegmentInfo>();
      @NotNull private final Map<SegmentInfo, DisposableIterator<List<SegmentInfo>>> myEndMarkers
        = new IdentityHashMap<SegmentInfo, DisposableIterator<List<SegmentInfo>>>();

      {
        for (DisposableIterator<List<SegmentInfo>> iterator : iterators) {
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
        DisposableIterator<List<SegmentInfo>> iterator = myEndMarkers.remove(result);
        if (iterator != null) {
          extract(iterator);
        }
        while (!myInfos.isEmpty()) {
          SegmentInfo toMerge = myInfos.peek();
          if (toMerge.startOffset != result.startOffset || toMerge.endOffset != result.endOffset) {
            break;
          }
          myInfos.remove();
          result = merge(result, toMerge);
          DisposableIterator<List<SegmentInfo>> it = myEndMarkers.remove(toMerge);
          if (it != null) {
            extract(it);
          }
        }
        return result;
      }

      @NotNull
      private SegmentInfo merge(@NotNull SegmentInfo info1, @NotNull SegmentInfo info2) {
        Color background = info1.background;
        if (background == null || defaultBackground.equals(background)) {
          background = info2.background;
        }
        
        Color foreground = info1.foreground;
        if (foreground == null || defaultForeground.equals(foreground)) {
          foreground = info2.foreground;
        }
        
        int fontStyle = info1.fontStyle;
        if (fontStyle == Font.PLAIN) {
          fontStyle = info2.fontStyle;
        }
        return new SegmentInfo(foreground, background, info1.fontFamilyName, fontStyle, info1.fontSize, info1.startOffset, info1.endOffset);
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      @Override
      public void dispose() {
        for (DisposableIterator<List<SegmentInfo>> iterator : iterators) {
          iterator.dispose();
        }
      }

      private void extract(@NotNull DisposableIterator<List<SegmentInfo>> iterator) {
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
  private static DisposableIterator<List<SegmentInfo>> wrap(@NotNull final EditorHighlighter highlighter,
                                                            @NotNull final Editor editor,
                                                            final int startOffset,
                                                            final int endOffset)
  {
    final HighlighterIterator highlighterIterator = highlighter.createIterator(startOffset);
    return new DisposableIterator<List<SegmentInfo>>() {

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

      @Override
      public void dispose() {
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

  @SuppressWarnings("unchecked")
  @NotNull
  private static DisposableIterator<List<SegmentInfo>> wrap(@NotNull MarkupModel model,
                                                            @NotNull final Editor editor,
                                                            @NotNull final EditorColorsScheme colorsScheme,
                                                            final int startOffset,
                                                            final int endOffset)
  {
    if (!(model instanceof MarkupModelEx)) {
      return DisposableIterator.EMPTY;
    }
    final DisposableIterator<RangeHighlighterEx> iterator = ((MarkupModelEx)model).overlappingIterator(startOffset, endOffset);
    final Color defaultForeground = colorsScheme.getDefaultForeground();
    final Color defaultBackground = colorsScheme.getDefaultBackground();
    return new DisposableIterator<List<SegmentInfo>>() {

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

      @Override
      public void dispose() {
        iterator.dispose();
      }

      private boolean updateCached() {
        if (!iterator.hasNext()) {
          return false;
        }
        
        RangeHighlighterEx highlighter = iterator.next();
        while (highlighter == null
               || !highlighter.isValid()
               || highlighter.getTextAttributes() == null
               || !isInterestedHighlightLayer(highlighter.getLayer()))
        {
          if (!iterator.hasNext()) {
            return false;
          }
          highlighter = iterator.next();
        }
        
        int tokenStart = Math.max(highlighter.getStartOffset(), startOffset);
        if (tokenStart >= endOffset) {
          return false;
        }
        
        TextAttributes attributes = null;
        Object tooltip = highlighter.getErrorStripeTooltip();
        if (tooltip instanceof HighlightInfo) {
          HighlightInfoType type = ((HighlightInfo)tooltip).type;
          if (type != null) {
            attributes = colorsScheme.getAttributes(type.getAttributesKey());
          }
        }
        if (attributes == null) {
          attributes = highlighter.getTextAttributes();
        }
        
        if (attributes == null) {
          return updateCached();
        }
        Color foreground = attributes.getForegroundColor();
        Color background = attributes.getBackgroundColor();
        if ((foreground == null || defaultForeground.equals(foreground))
            && (background == null || defaultBackground.equals(background))
            && attributes.getFontType() == Font.PLAIN)
        {
          return updateCached();
        }
        
        int tokenEnd = Math.min(highlighter.getEndOffset(), endOffset);
        //noinspection ConstantConditions
        myCached = SegmentInfo.produce(attributes, editor, tokenStart, tokenEnd);
        return true;
      }
      
      private boolean isInterestedHighlightLayer(int layer) {
        return layer == HighlighterLayer.SYNTAX || layer == HighlighterLayer.ADDITIONAL_SYNTAX;
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

    @NotNull private final CharSequence myText;
    @NotNull private final Color        myDefaultForeground;
    @NotNull private final Color        myDefaultBackground;

    @Nullable private Color  myBackground;
    @Nullable private Color  myForeground;
    @Nullable private String myFontFamilyName;

    private int myFontStyle;
    private int myFontSize;
    private int myStartOffset;
    private int myOffsetShift;

    Context(@NotNull Editor editor, @NotNull EditorColorsScheme scheme) {
      myText = editor.getDocument().getCharsSequence();
      myDefaultForeground = scheme.getDefaultForeground();
      myDefaultBackground = scheme.getDefaultBackground();
    }

    public void reset(int offsetShift) {
      myBackground = null;
      myForeground = null;
      myFontFamilyName = null;
      myFontStyle = Font.PLAIN;
      myFontSize = -1;
      myStartOffset = -1;
      myOffsetShift = offsetShift;
    }

    public void onNewData(@NotNull SegmentInfo info) {
      if (myStartOffset < 0) {
        onFirstSegment(info);
        return;
      }

      if (containsWhiteSpacesOnly(info)) {
        return;
      }

      processBackground(info);
      processForeground(info);
      processFontFamilyName(info);
      processFontStyle(info);
      processFontSize(info);
    }

    private boolean containsWhiteSpacesOnly(@NotNull SegmentInfo info) {
      for (int i = info.startOffset, limit = info.endOffset; i < limit; i++) {
        char c = myText.charAt(i);
        if (c != ' ' && c != '\t' && c != '\n') {
          return false;
        }
      }
      return true;
    }

    private void processFontStyle(@NotNull SegmentInfo info) {
      if (info.fontStyle != myFontStyle) {
        addTextIfPossible(info.startOffset);
        myOutputInfos.add(new FontStyle(info.fontStyle));
        myFontStyle = info.fontStyle;
      }
    }

    private void processFontSize(@NotNull SegmentInfo info) {
      if (info.fontSize != myFontSize) {
        addTextIfPossible(info.startOffset);
        myOutputInfos.add(new FontSize(info.fontSize));
        myFontSize = info.fontSize;
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
          myOutputInfos.add(new Foreground(myColorRegistry.getId(c)));
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

      myFontSize = info.fontSize;
      if (myFontSize > 0) {
        myOutputInfos.add(new FontSize(myFontSize));
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
      int foreground = myColorRegistry.getId(myDefaultForeground);
      int background = myColorRegistry.getId(myDefaultBackground);
      myColorRegistry.seal();
      myFontNameRegistry.seal();
      return new SyntaxInfo(myOutputInfos, foreground, background, myFontNameRegistry, myColorRegistry);
    }
  }

  private static class SegmentInfo implements Comparable<SegmentInfo> {

    @Nullable public final Color  foreground;
    @Nullable public final Color  background;
    @NotNull public final  String fontFamilyName;

    public final int fontStyle;
    public final int fontSize;
    public final int startOffset;
    public final int endOffset;

    SegmentInfo(@Nullable Color foreground,
                @Nullable Color background,
                @NotNull String fontFamilyName,
                int fontStyle,
                int fontSize,
                int startOffset,
                int endOffset)
    {
      this.foreground = foreground;
      this.background = background;
      this.fontFamilyName = fontFamilyName;
      this.fontStyle = fontStyle;
      this.fontSize = fontSize;
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
      Font font = EditorUtil.fontForChar(text.charAt(start), attribute.getFontType(), editor).getFont();
      String currentFontFamilyName = font.getFamily();
      int currentFontSize = font.getSize();
      String candidateFontFamilyName;
      int candidateFontSize;
      for (int i = start + 1; i < end; i++) {
        font = EditorUtil.fontForChar(text.charAt(i), attribute.getFontType(), editor).getFont();
        candidateFontFamilyName = font.getFamily();
        candidateFontSize = font.getSize();
        if (!candidateFontFamilyName.equals(currentFontFamilyName) || currentFontSize != candidateFontSize) {
          result.add(new SegmentInfo(attribute.getForegroundColor(),
                                     attribute.getBackgroundColor(),
                                     currentFontFamilyName,
                                     attribute.getFontType(),
                                     currentFontSize,
                                     currentStart,
                                     i
          ));
          currentStart = i;
          currentFontFamilyName = candidateFontFamilyName;
          currentFontSize = candidateFontSize;
        }
      }

      if (currentStart < end) {
        result.add(new SegmentInfo(attribute.getForegroundColor(),
                                   attribute.getBackgroundColor(),
                                   currentFontFamilyName,
                                   attribute.getFontType(),
                                   currentFontSize,
                                   currentStart,
                                   end
        ));
      }

      return result;
    }

    @Override
    public int compareTo(@NotNull SegmentInfo o) {
      return startOffset - o.startOffset;
    }

    @Override
    public int hashCode() {
      int result = foreground != null ? foreground.hashCode() : 0;
      result = 31 * result + (background != null ? background.hashCode() : 0);
      result = 31 * result + fontFamilyName.hashCode();
      result = 31 * result + fontStyle;
      result = 31 * result + fontSize;
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
      if (fontSize != info.fontSize) return false;
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
