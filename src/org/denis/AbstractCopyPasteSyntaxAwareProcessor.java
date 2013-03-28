package org.denis;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.editorActions.CopyPastePostProcessor;
import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.ex.DisposableIterator;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.impl.ComplementaryFontsRegistry;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
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

  private static final Logger LOG = Logger.getInstance("#" + AbstractCopyPasteSyntaxAwareProcessor.class.getName());
  
  private static ThreadLocal<Pair<Long, SyntaxInfo>> CACHED = new ThreadLocal<Pair<Long, SyntaxInfo>>();
  private static final long CACHE_TTL_MS = 100;

  @SuppressWarnings("unchecked")
  @Nullable
  @Override
  public T collectTransferableData(PsiFile file, Editor editor, int[] startOffsets, int[] endOffsets) {
    CopyOnSteroidSettings settings = CopyOnSteroidSettings.getInstance();
    if (!isEnabled(settings) || startOffsets.length <= 0) {
      return null;
    }

    Pair<Long, SyntaxInfo> pair = CACHED.get();
    if (pair != null && System.currentTimeMillis() - pair.first < CACHE_TTL_MS) {
      return build(pair.second);
    }

    SelectionModel selectionModel = editor.getSelectionModel();
    LogicalPosition blockStart = selectionModel.getBlockStart();
    LogicalPosition blockEnd = selectionModel.getBlockEnd();
    final int indentSymbolsToStrip;
    final int firstLineStartOffset;
    final int lineWidth;
    if (blockStart != null && blockEnd != null) {
      lineWidth = Math.abs(blockEnd.column - blockStart.column);
      indentSymbolsToStrip = 0;
      firstLineStartOffset = startOffsets[0];
    }
    else {
      lineWidth = -1;
      if (settings.isStripIndents()) {
        Pair<Integer, Integer> p = calcIndentSymbolsToStrip(editor.getDocument(), startOffsets[0], endOffsets[endOffsets.length - 1]);
        firstLineStartOffset = p.first;
        indentSymbolsToStrip = p.second;
      }
      else {
        firstLineStartOffset = startOffsets[0];
        indentSymbolsToStrip = 0;
      }
    }
    logInitial(editor, startOffsets, endOffsets, indentSymbolsToStrip, firstLineStartOffset, lineWidth);
    CharSequence text = editor.getDocument().getCharsSequence();
    EditorHighlighter highlighter = HighlighterFactory.createHighlighter(file.getProject(), file.getVirtualFile());
    highlighter.setText(text);
    EditorColorsScheme schemeToUse = settings.getColorsScheme(editor);
    highlighter.setColorScheme(schemeToUse);
    MarkupModel markupModel = DocumentMarkupModel.forDocument(editor.getDocument(), file.getProject(), false);
    Context context = new Context(editor, schemeToUse, indentSymbolsToStrip);
    int shift = 0;
    int prevEndOffset = 0;
    
    for (int i = 0; i < startOffsets.length; i++) {
      int startOffsetToUse = i == 0 ? firstLineStartOffset : startOffsets[i];
      if (i > 0) { // Block selection is active.
        int fillStringLength = lineWidth - (endOffsets[i - 1] - startOffsets[i - 1]); // Block selection fills short lines by white spaces.
        int endLineOffset = endOffsets[i - 1] + shift + fillStringLength;
        context.outputInfos.add(new Text(endLineOffset, endLineOffset + 1));
        shift++; // Block selection ends '\n' at line end
        shift += fillStringLength;
      }
      shift += prevEndOffset - startOffsets[i];
      prevEndOffset = endOffsets[i];
      context.reset(shift);
      DisposableIterator<SegmentInfo> it = aggregateSyntaxInfo(editor,
                                                               wrap(highlighter, editor, schemeToUse, startOffsetToUse, endOffsets[i]),
                                                               wrap(markupModel, editor, schemeToUse, startOffsetToUse, endOffsets[i]));
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
    SyntaxInfo syntaxInfo = context.finish();
    logSyntaxInfo(syntaxInfo);
    CACHED.set(Pair.create(System.currentTimeMillis(), syntaxInfo));
    return build(syntaxInfo);
  }

  private static void logInitial(@NotNull Editor editor,
                                 @NotNull int[] startOffsets,
                                 @NotNull int[] endOffsets,
                                 int indentSymbolsToStrip,
                                 int firstLineStartOffset,
                                 int lineWidth)
  {
    CopyOnSteroidSettings settings = CopyOnSteroidSettings.getInstance();
    if (!settings.isDebugProcessing()) {
      return;
    }
    
    StringBuilder buffer = new StringBuilder();
    Document document = editor.getDocument();
    CharSequence text = document.getCharsSequence();
    for (int i = 0; i < startOffsets.length; i++) {
      int start = startOffsets[i];
      int lineStart = document.getLineStartOffset(document.getLineNumber(start));
      int end = endOffsets[i];
      int lineEnd = document.getLineEndOffset(document.getLineNumber(end));
      buffer.append("    region #").append(i).append(": ").append(start).append('-').append(end).append(", text at range ")
        .append(lineStart).append('-').append(lineEnd).append(": \n'").append(text.subSequence(lineStart, lineEnd)).append("'\n");
    }
    if (buffer.length() > 0) {
      buffer.setLength(buffer.length() - 1);
    }
    LOG.info(String.format(
      "Preparing syntax-aware text. Given: %s selection, indent symbols to strip=%d, first line start offset=%d, line width=%d, "
      + "selected text:%n%s",
      startOffsets.length > 1 ? "block" : "regular", indentSymbolsToStrip, firstLineStartOffset, lineWidth, buffer
    ));
  }

  private static void logSyntaxInfo(@NotNull SyntaxInfo info) {
    CopyOnSteroidSettings settings = CopyOnSteroidSettings.getInstance();
    if (!settings.isDebugProcessing()) {
      return;
    }
    LOG.info("Constructed syntax info: " + info);
  }

  private static Pair<Integer/* start offset to use */, Integer /* indent symbols to strip */> calcIndentSymbolsToStrip(
    @NotNull Document document, int startOffset, int endOffset)
  {
    int startLine = document.getLineNumber(startOffset);
    int endLine = document.getLineNumber(endOffset);
    CharSequence text = document.getCharsSequence();
    for (int line = startLine; line <= endLine; line++) {
      int lineStartOffset = document.getLineStartOffset(line);
      int lineEndOffset = document.getLineEndOffset(line);
      int nonWsOffset = lineEndOffset;
      for (int i = lineStartOffset; i < lineEndOffset; i++) {
        char c = text.charAt(i);
        if (c != ' ' && c != '\t') {
          nonWsOffset = i;
          break;
        }
      }
      if (nonWsOffset >= lineEndOffset) {
        continue; // Blank line
      }
      final int startOffsetToUse;
      if (line == startLine && nonWsOffset > startOffset) {
        startOffsetToUse = nonWsOffset;
      }
      else {
        startOffsetToUse = startOffset;
      }
      return Pair.create(startOffsetToUse, nonWsOffset - lineStartOffset);
    }
    return Pair.create(startOffset, 0);
  }

  @Nullable
  protected abstract T build(@NotNull SyntaxInfo info);

  protected abstract boolean isEnabled(@NotNull CopyOnSteroidSettings settings);

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
          if (toMerge.endOffset > result.endOffset) {
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
                                                            @NotNull final EditorColorsScheme colorsScheme,
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

        if (highlighterIterator.getTokenType() == TokenType.BAD_CHARACTER) {
          // Skip syntax errors.
          highlighterIterator.advance();
          return updateCached();
        }
        TextAttributes attributes = highlighterIterator.getTextAttributes();
        int tokenEnd = Math.min(highlighterIterator.getEnd(), endOffset);
        myCached = SegmentInfo.produce(attributes, editor, colorsScheme, tokenStart, tokenEnd);
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
          HighlightInfo info = (HighlightInfo)tooltip;
          TextAttributesKey key = info.forcedTextAttributesKey;
          if (key == null) {
            HighlightInfoType type = info.type;
            if (type != null) {
              key = type.getAttributesKey();
            }
          }
          if (key != null) {
            attributes = colorsScheme.getAttributes(key);
          }
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
        myCached = SegmentInfo.produce(attributes, editor, colorsScheme, tokenStart, tokenEnd);
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

    @NotNull public final List<OutputInfo> outputInfos = ContainerUtilRt.newArrayList();

    @NotNull private final ColorRegistry    myColorRegistry    = new ColorRegistry();
    @NotNull private final FontNameRegistry myFontNameRegistry = new FontNameRegistry();

    @NotNull private final CharSequence myText;
    @NotNull private final Color        myDefaultForeground;
    @NotNull private final Color        myDefaultBackground;

    @Nullable private Color  myBackground;
    @Nullable private Color  myForeground;
    @Nullable private String myFontFamilyName;
    
    private final int myIndentSymbolsToStrip;
    
    private int myFontStyle   = -1;
    private int myFontSize    = -1;
    private int myStartOffset = -1;
    private int myOffsetShift = -1;
    
    private int mySingleFontSize;
    
    private int myIndentSymbolsToStripAtCurrentLine;

    Context(@NotNull Editor editor, @NotNull EditorColorsScheme scheme, int indentSymbolsToStrip) {
      myText = editor.getDocument().getCharsSequence();
      myDefaultForeground = scheme.getDefaultForeground();
      myDefaultBackground = scheme.getDefaultBackground();
      myIndentSymbolsToStrip = indentSymbolsToStrip;
    }

    public void reset(int offsetShift) {
      myStartOffset = -1;
      myOffsetShift = offsetShift;
      myIndentSymbolsToStripAtCurrentLine = 0;
    }

    public void onNewData(@NotNull SegmentInfo info) {
      if (myStartOffset < 0) {
        myStartOffset = info.startOffset;
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
        outputInfos.add(new FontStyle(info.fontStyle));
        myFontStyle = info.fontStyle;
      }
    }

    private void processFontSize(@NotNull SegmentInfo info) {
      if (mySingleFontSize == 0) {
        mySingleFontSize = info.fontSize;
      }
      else if (mySingleFontSize > 0 && mySingleFontSize != info.fontSize) {
        mySingleFontSize = -1;
      }
      if (info.fontSize != myFontSize) {
        addTextIfPossible(info.startOffset);
        outputInfos.add(new FontSize(info.fontSize));
        myFontSize = info.fontSize;
      }
    }

    private void processFontFamilyName(@NotNull SegmentInfo info) {
      if (!info.fontFamilyName.equals(myFontFamilyName)) {
        addTextIfPossible(info.startOffset);
        outputInfos.add(new FontFamilyName(myFontNameRegistry.getId(info.fontFamilyName)));
        myFontFamilyName = info.fontFamilyName;
      }
    }

    private void processForeground(@NotNull SegmentInfo info) {
      if (myForeground == null && info.foreground != null) {
        addTextIfPossible(info.startOffset);
        myForeground = info.foreground;
        outputInfos.add(new Foreground(myColorRegistry.getId(info.foreground)));
      }
      else if (myForeground != null) {
        Color c = info.foreground == null ? myDefaultForeground : info.foreground;
        if (!myForeground.equals(c)) {
          addTextIfPossible(info.startOffset);
          outputInfos.add(new Foreground(myColorRegistry.getId(c)));
          myForeground = c;
        }
      }
    }

    private void processBackground(@NotNull SegmentInfo info) {
      if (myBackground == null && info.background != null) {
        addTextIfPossible(info.startOffset);
        myBackground = info.background;
        outputInfos.add(new Background(myColorRegistry.getId(info.background)));
      }
      else if (myBackground != null) {
        Color c = info.background == null ? myDefaultBackground : info.background;
        if (!myBackground.equals(c)) {
          addTextIfPossible(info.startOffset);
          outputInfos.add(new Background(myColorRegistry.getId(myBackground)));
          myBackground = c;
        }
      }
    }

    private void addTextIfPossible(int endOffset) {
      if (endOffset <= myStartOffset) {
        return;
      }

      for (int i = myStartOffset; i < endOffset; i++) {
        char c = myText.charAt(i);
        switch (c) {
          case '\n':
            myIndentSymbolsToStripAtCurrentLine = myIndentSymbolsToStrip;
            outputInfos.add(new Text(myStartOffset + myOffsetShift, i + myOffsetShift + 1));
            myStartOffset = i + 1;
            break;
          // Intended fall-through.
          case ' ':
          case '\t':
            if (myIndentSymbolsToStripAtCurrentLine > 0) {
              myIndentSymbolsToStripAtCurrentLine--;
              myStartOffset++;
              continue;
            }
          default: myIndentSymbolsToStripAtCurrentLine = 0;
        }
      }

      if (myStartOffset < endOffset) {
        outputInfos.add(new Text(myStartOffset + myOffsetShift, endOffset + myOffsetShift));
        myStartOffset = endOffset;
      }
    }

    public void onIterationEnd(int endOffset) {
      addTextIfPossible(endOffset);
    }

    @NotNull
    public SyntaxInfo finish() {
      int foreground = myColorRegistry.getId(myDefaultForeground);
      int background = myColorRegistry.getId(myDefaultBackground);
      myColorRegistry.seal();
      myFontNameRegistry.seal();
      return new SyntaxInfo(outputInfos, foreground, background, mySingleFontSize, myFontNameRegistry, myColorRegistry);
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
    public static List<SegmentInfo> produce(@NotNull TextAttributes attribute,
                                            @NotNull Editor editor,
                                            @NotNull EditorColorsScheme colorsScheme,
                                            int start,
                                            int end)
    {
      if (end <= start) {
        return Collections.emptyList();
      }
      List<SegmentInfo> result = ContainerUtilRt.newArrayList();
      CharSequence text = editor.getDocument().getCharsSequence();
      int currentStart = start;
      int fontSize = colorsScheme.getEditorFontSize();
      int fontStyle = attribute.getFontType();
      String defaultFontFamily = colorsScheme.getEditorFontName();
      Font font = ComplementaryFontsRegistry.getFontAbleToDisplay(text.charAt(start), fontSize, fontStyle, defaultFontFamily).getFont();
      String currentFontFamilyName = font.getFamily();
      int currentFontSize = font.getSize();
      String candidateFontFamilyName;
      int candidateFontSize;
      for (int i = start + 1; i < end; i++) {
        font = ComplementaryFontsRegistry.getFontAbleToDisplay(text.charAt(i), fontSize, fontStyle, defaultFontFamily).getFont();
        candidateFontFamilyName = font.getFamily();
        candidateFontSize = font.getSize();
        if (!candidateFontFamilyName.equals(currentFontFamilyName) || currentFontSize != candidateFontSize) {
          result.add(new SegmentInfo(attribute.getForegroundColor(),
                                     attribute.getBackgroundColor(),
                                     currentFontFamilyName,
                                     fontStyle,
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
                                   fontStyle,
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
