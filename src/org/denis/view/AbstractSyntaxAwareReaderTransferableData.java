package org.denis.view;

import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.util.StringBuilderSpinAllocator;
import org.denis.model.SyntaxInfo;
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
//      myDelegate = new StringReader(buffer.toString());
      myDelegate = new StringReader(
        "<pre style=\"background: #ffffff; border: 1px solid #000000; padding: 0.04in; line-height: 100%\"><font color=\"#000080\">package </font></pre>"
      );
//      myDelegate = new StringReader(
//        "<pre style=\"background: #ffffff; border: 1px solid #000000; padding: 0.04in; line-height: 100%\"><font face=\"monospace\"><font size=3><font color=\"#000080\"><b><span style=\"background: #ffffff\">package</span></b></font><span style=\"background: #ffffff\"> org.gradle.webservice;</span></font></font><font face=\"monospace\"><font size=3><font color=\"#000080\"><b><span style=\"background: #ffffff\"><br/>import</span></b></font><span style=\"background: #ffffff\"> org.apache.commons.io.filenameutils;</span></font></font><font face=\"monospace\"><font size=3><font color=\"#000080\"><b><span style=\"background: #ffffff\">import</span></b></font><span style=\"background: #ffffff\"> org.apache.commons.lang.builder.tostringbuilder;</span></font></font><font face=\"monospace\"><font size=3><font color=\"#000080\"><b><span style=\"background: #ffffff\">import</span></b></font><span style=\"background: #ffffff\"> org.apache.commons.collections.list.growthlist;</span></font></font><font face=\"monospace\"><font size=3><font color=\"#000080\"><b><span style=\"background: #ffffff\">import</span></b></font><span style=\"background: #ffffff\"> org.gradle.shared.person;</span></font></font><font face=\"monospace\"><font size=3><font color=\"#000080\"><b><span style=\"background: #ffffff\">import</span></b></font><span style=\"background: #ffffff\"> org.gradle.api.personlist;</span></font></font><font face=\"monospace\"><font size=3><font color=\"#000080\"><b><span style=\"background: #ffffff\">public</span></b></font><span style=\"background: #ffffff\"> </span><font color=\"#000080\"><b><span style=\"background: #ffffff\">class</span></b></font><span style=\"background: #ffffff\"> testtest {</span></font></font><span style=\"background: #ffffff\">    </span><font color=\"#000080\"><font face=\"monospace\"><font size=3><b><span style=\"background: #ffffff\">private</span></b></font></font></font><font face=\"monospace\"><font size=3><span style=\"background: #ffffff\"> string name;</span></font></font><span style=\"background: #ffffff\">    </span><font color=\"#000080\"><font face=\"monospace\"><font size=3><b><span style=\"background: #ffffff\">public</span></b></font></font></font><font face=\"monospace\"><font size=3><span style=\"background: #ffffff\"> </span></font></font><font color=\"#000080\"><font face=\"monospace\"><font size=3><b><span style=\"background: #ffffff\">void</span></b></font></font></font><font face=\"monospace\"><font size=3><span style=\"background: #ffffff\"> method() {</span></font></font><span style=\"background: #ffffff\">        </span><font face=\"monospace\"><font size=3><span style=\"background: #ffffff\">filenameutils.separatorstounix(</span></font></font><font color=\"#008000\"><font face=\"monospace\"><font size=3><b><span style=\"background: #ffffff\">&quot;my/unix/filename&quot;</span></b></font></font></font><font face=\"monospace\"><font size=3><span style=\"background: #ffffff\">);</span></font></font><span style=\"background: #ffffff\">        </span><font face=\"monospace\"><font size=3><span style=\"background: #ffffff\">tostringbuilder.reflectiontostring(</span></font></font><font color=\"#000080\"><font face=\"monospace\"><font size=3><b><span style=\"background: #ffffff\">new</span></b></font></font></font><font face=\"monospace\"><font size=3><span style=\"background: #ffffff\"> person(</span></font></font><font color=\"#008000\"><font face=\"monospace\"><font size=3><b><span style=\"background: #ffffff\">&quot;name&quot;</span></b></font></font></font><font face=\"monospace\"><font size=3><span style=\"background: #ffffff\">));</span></font></font><span style=\"background: #ffffff\">        </span><font color=\"#000080\"><font face=\"monospace\"><font size=3><b><span style=\"background: #ffffff\">new</span></b></font></font></font><font face=\"monospace\"><font size=3><span style=\"background: #ffffff\"> growthlist();</span></font></font><span style=\"background: #ffffff\">        </span><font color=\"#000080\"><font face=\"monospace\"><font size=3><b><span style=\"background: #ffffff\">new</span></b></font></font></font><font face=\"monospace\"><font size=3><span style=\"background: #ffffff\"> personlist().dosomethingwithimpl(); </span></font></font><font color=\"#808080\"><font face=\"monospace\"><font size=3><i><span style=\"background: #ffffff\">// compile with api-spi, runtime with api</span></i></font></font></font><span style=\"background: #ffffff\">    <font face=\"monospace\"><font size=3>}</font></font></span><font face=\"monospace\"><font size=3><span style=\"background: #ffffff\">}</span></font></font></pre>"
//      );
      return myDelegate;
    }
    finally {
      StringBuilderSpinAllocator.dispose(buffer);
    }
  }

  protected abstract void build(@NotNull SyntaxInfo syntaxInfo, @NotNull String rawText, @NotNull StringBuilder holder);
}
