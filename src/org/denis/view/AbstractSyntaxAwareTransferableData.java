package org.denis.view;

import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.util.StringBuilderSpinAllocator;
import org.denis.model.SyntaxInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * @author Denis Zhdanov
 * @since 3/28/13 1:20 PM
 */
public abstract class AbstractSyntaxAwareTransferableData extends InputStream implements TextBlockTransferableData, Serializable {

  @NotNull private final SyntaxInfo mySyntaxInfo;

  @Nullable private transient InputStream myDelegate;

  public AbstractSyntaxAwareTransferableData(@NotNull SyntaxInfo syntaxInfo) {
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
  public int read(@NotNull byte[] b, int off, int len) throws IOException {
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
      build(mySyntaxInfo, rawText, buffer);
      myDelegate = new ByteArrayInputStream(buffer.toString().getBytes());
      return myDelegate;
    }
    finally {
      StringBuilderSpinAllocator.dispose(buffer);
    }
  }
  
  protected abstract void build(@NotNull SyntaxInfo syntaxInfo, @NotNull String rawText, @NotNull StringBuilder holder);
}
