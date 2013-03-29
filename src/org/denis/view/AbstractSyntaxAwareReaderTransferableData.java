package org.denis.view;

import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.util.StringBuilderSpinAllocator;
import org.denis.model.SyntaxInfo;
import org.denis.settings.CopyOnSteroidSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.*;

/**
 * @author Denis Zhdanov
 * @since 3/28/13 7:09 PM
 */
public abstract class AbstractSyntaxAwareReaderTransferableData extends Reader
  implements TextBlockTransferableData, Serializable
{

  private static final Logger LOG = Logger.getInstance("#" + AbstractSyntaxAwareReaderTransferableData.class.getName());

  @NotNull private final SyntaxInfo mySyntaxInfo;

  @Nullable private transient Reader myDelegate;

  public AbstractSyntaxAwareReaderTransferableData(@NotNull SyntaxInfo syntaxInfo) {
    mySyntaxInfo = syntaxInfo;
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
  public int read(char[] cbuf, int off, int len) throws IOException {
    return getDelegate().read(cbuf, off, len);
  }

  @Override
  public void close() throws IOException {
    myDelegate = null;
  }

  @NotNull
  private Reader getDelegate() {
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
      build(mySyntaxInfo, rawText, buffer);
      String s = buffer.toString();
      if (CopyOnSteroidSettings.getInstance().isDebugProcessing()) {
        LOG.info("Resulting text: \n'" + s + "'");
      }
      myDelegate = new StringReader(s);
      return myDelegate;
    }
    finally {
      StringBuilderSpinAllocator.dispose(buffer);
    }
  }

  protected abstract void build(@NotNull SyntaxInfo syntaxInfo, @NotNull String rawText, @NotNull StringBuilder holder);
}
