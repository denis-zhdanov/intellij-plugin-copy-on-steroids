package org.denis;

import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.util.Ref;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.util.containers.ContainerUtilRt;
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
    // Block selection ends implicit line feeds at line end and fills short line to max line width by white spaces.
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

  public void testRegularSelection() {
    // We want to exclude unnecessary indents from the pasted results.
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
    myFixture.configureByText("test.java", text);

    int selectionStart = text.indexOf("public int");
    int selectionEnd = text.indexOf('}', selectionStart) + 1;
    SelectionModel selectionModel = myFixture.getEditor().getSelectionModel();
    selectionModel.setSelection(selectionStart, selectionEnd);

    List<OutputInfo> expected = Arrays.asList(
      new Foreground(1), new FontFamilyName(1), new FontStyle(Font.BOLD), new FontSize(12), new Text(0, 11), // 'public int '
      new Foreground(2), new FontStyle(Font.PLAIN), new Text(11, 24), // 'getField() {\n'
      new Text(28, 32), // '    ' - indent before 'return field;'
      new Foreground(1), new FontStyle(Font.BOLD), new Text(32, 39), // 'return '
      new Foreground(3), new Text(39, 44), // 'field'
      new Foreground(2), new FontStyle(Font.PLAIN), new Text(44, 46), // ';\n'
      new Text(50, 51) // '}'
    );
    
    assertEquals(expected, getSyntaxInfoForRegularSelection().getOutputInfos());
    
    selectionModel.setSelection(selectionStart - 2, selectionEnd);
    assertEquals(shiftText(expected, 2), getSyntaxInfoForRegularSelection().getOutputInfos());

    selectionModel.setSelection(selectionStart - 4, selectionEnd);
    assertEquals(shiftText(expected, 4), getSyntaxInfoForRegularSelection().getOutputInfos());
  }
  
  @NotNull
  private static List<OutputInfo> shiftText(@NotNull List<OutputInfo> base, final int offsetShift) {
    final List<OutputInfo> result = ContainerUtilRt.newArrayList();
    OutputInfoVisitor visitor = new OutputInfoVisitor() {
      @Override
      public void visit(@NotNull Text text) {
        result.add(new Text(text.getStartOffset() + offsetShift, text.getEndOffset() + offsetShift));
      }

      @Override
      public void visit(@NotNull Foreground color) {
        result.add(color);
      }

      @Override
      public void visit(@NotNull Background color) {
        result.add(color);
      }

      @Override
      public void visit(@NotNull FontFamilyName name) {
        result.add(name);
      }

      @Override
      public void visit(@NotNull FontStyle style) {
        result.add(style);
      }

      @Override
      public void visit(@NotNull FontSize size) {
        result.add(size);
      }
    };
    for (OutputInfo info : base) {
      info.invite(visitor);
    }
    return result;
  }
  
  @NotNull
  private SyntaxInfo getSyntaxInfoForRegularSelection() {
    SelectionModel model = myFixture.getEditor().getSelectionModel();
    assertTrue(model.hasSelection());
    return doGetSyntaxInfo(new int[] { model.getSelectionStart() }, new int[] { model.getSelectionEnd() });
  }
  
  @NotNull
  private SyntaxInfo getSyntaxInfoForBlockSelection() {
    SelectionModel model = myFixture.getEditor().getSelectionModel();
    return doGetSyntaxInfo(model.getBlockSelectionStarts(), model.getBlockSelectionEnds());
  }
  
  private SyntaxInfo doGetSyntaxInfo(int[] startOffsets, int[] endOffsets) {
    myFixture.doHighlighting();
    final Ref<SyntaxInfo> syntaxInfo = new Ref<SyntaxInfo>();
    Editor editor = myFixture.getEditor();

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
    }.collectTransferableData(myFixture.getFile(), editor, startOffsets, endOffsets);
    
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
