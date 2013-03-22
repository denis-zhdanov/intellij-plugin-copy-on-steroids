package org.denis;

import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;
import java.io.*;

public class SyntaxAwareTransferableData extends InputStream implements TextBlockTransferableData, Serializable {

  private static final long   serialVersionUID = 1L;
  private static final String CHARSET          = "UTF-8";

  private static final DataFlavor FLAVOR = new DataFlavor("text/rtf;class=java.io.InputStream;charset=" + CHARSET, "RTF text");

  @NotNull private final byte[] myData;
  @Nullable private transient ByteArrayInputStream myStream;

  public SyntaxAwareTransferableData(@NotNull String data) {
    try {
      myData = data.getBytes(CHARSET);
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public DataFlavor getFlavor() {
    return FLAVOR;
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
    return getStream().read();
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return getStream().read(b, off, len);
  }

  @NotNull
  private InputStream getStream() {
    if (myStream == null) {
      myStream = new ByteArrayInputStream(myData);
    }
    return myStream;
  }
}
