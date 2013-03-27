package org.denis;

import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.util.Ref;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.denis.model.*;
import org.denis.settings.CopyOnSteroidSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.util.Arrays;
import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 3/27/13 11:11 AM
 */
public class SyntaxInfoConstructionTest extends LightCodeInsightFixtureTestCase {

  public void testBlockSelection() {
    String text =
      "package org;\n" +
      "\n" +
      "public class TestClass {\n" +
      "\n" +
      "    int field;\n" +
      "\n" +
      "    public int getField() {\n" +
      "        return field;\n" +
      "    }\n" +
      "}";
    myFixture.configureByText("Test.java", text);

    List<OutputInfo> expected = Arrays.asList(
      new Foreground(1), new FontFamilyName(1), new FontStyle(Font.BOLD), new FontSize(12), new Text(0, 11), // 'public int '
      new Foreground(2), new FontStyle(Font.PLAIN), new Text(11, 23), // 'getField() {'
      new Text(23, 24), // '\n'
      new Text(24, 28), // '    ' - indent before 'return field;'
      new Foreground(1), new FontStyle(Font.BOLD), new Text(28, 35), // 'return '
      new Foreground(3), new Text(35, 40), // 'field';
      new Foreground(2), new FontStyle(Font.PLAIN), new Text(40, 41), // ';'
      new Text(47, 48), // '\n'
      new Text(48, 49) // '}'
    );

    int blockSelectionStartOffset = text.indexOf("public int");
    Editor editor = myFixture.getEditor();
    LogicalPosition blockSelectionStartPosition = editor.offsetToLogicalPosition(blockSelectionStartOffset);
    LogicalPosition blockSelectionEndPosition = new LogicalPosition(
      blockSelectionStartPosition.line + 2,
      editor.offsetToLogicalPosition(text.indexOf('{', blockSelectionStartOffset)).column + 1);
    editor.getSelectionModel().setBlockSelection(blockSelectionStartPosition, blockSelectionEndPosition);
    assertEquals(expected, getSyntaxInfoForBlockSelection().getOutputInfos());
  }

  @NotNull
  private SyntaxInfo getSyntaxInfoForBlockSelection() {
    myFixture.doHighlighting();
    final Ref<SyntaxInfo> syntaxInfo = new Ref<SyntaxInfo>();
    Editor editor = myFixture.getEditor();
    SelectionModel selectionModel = editor.getSelectionModel();

    new AbstractCopyPasteSyntaxAwareProcessor<DummyTransferable>() {
      @Nullable
      @Override
      protected DummyTransferable build(@NotNull SyntaxInfo info) {
        syntaxInfo.set(info);
        return null;
      }

      @Override
      protected boolean isEnabled(@NotNull CopyOnSteroidSettings settings) {
        return true;
      }
    }.collectTransferableData(myFixture.getFile(), editor, selectionModel.getBlockSelectionStarts(), selectionModel.getBlockSelectionEnds());
    return syntaxInfo.get();
  }
  
  private static class DummyTransferable implements TextBlockTransferableData {
    @Override
    public DataFlavor getFlavor() {
      return null;
    }

    @Override
    public int getOffsetCount() {
      return 0;
    }

    @Override
    public int getOffsets(int[] offsets, int index) {
      return 0;
    }

    @Override
    public int setOffsets(int[] offsets, int index) {
      return 0;
    }
  }
}
