package org.denis;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Denis Zhdanov
 * @since 3/25/13 2:43 PM
 */
public class AAA {
  public static void main(String[] args) throws Exception {
    JFrame frame = new JFrame();
    JButton button = new JButton("click me");
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          store(1);
        }
        catch (Exception e1) {
          e1.printStackTrace();
        }
      }
    });
    frame.getContentPane().add(button);
    frame.setSize(640, 480);
    frame.setLocation(640, 480);
    frame.setVisible(true);
    
//    for (DataFlavor flavor : clipboard.getAvailableDataFlavors()) {
//      String mimeType = flavor.getMimeType();
//      if (mimeType == null || (!mimeType.contains("rtf") && !mimeType.contains("rich"))) {
//        continue;
//      }
//      Object data = clipboard.getData(flavor);
//      if (!(data instanceof InputStream)) {
//        continue;
//      }
//
//      InputStream in = (InputStream)data;
//      BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(in)));
//      StringBuilder buffer = new StringBuilder();
//      String line;
//      while ((line = reader.readLine()) != null) {
//        buffer.append(line);
//      }
//      System.out.println(buffer.toString());
//      return;
//    }
  }

  private static void store(final int i) throws Exception {
    final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    final Set<DataFlavor> flavors = new LinkedHashSet<DataFlavor>();
//    for (DataFlavor flavor : clipboard.getAvailableDataFlavors()) {
//      if (!flavor.getMimeType().contains("html")) {
//        continue;
//      }
//      flavors.add(flavor);
//    }
//    StringBuilder buffer = new StringBuilder();
//    BufferedReader reader = new BufferedReader((Reader)clipboard.getData(clipboard.getAvailableDataFlavors()[3]));
//    String line;
//    while ((line = reader.readLine()) != null) {
//      buffer.append(line);
//    }
    flavors.add(new DataFlavor("text/html;class=java.io.Reader"));
      
    clipboard.setContents(new Transferable() {
                            @Override
                            public DataFlavor[] getTransferDataFlavors() {
                              return flavors.toArray(new DataFlavor[flavors.size()]);
                            }

                            @Override
                            public boolean isDataFlavorSupported(DataFlavor flavor) {
                              return flavors.contains(flavor);
                            }

                            @Override
                            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                              final String text = "<span style='color:#000080;'>xxxxx</span>";
                              Class<?> aClass = flavor.getRepresentationClass();
                              if ("java.io.InputStream".equals(aClass.getName())) {
                                return new ByteArrayInputStream(text.getBytes());
                              }
                              else if ("java.io.Reader".equals(aClass.getName())) {
                                return new StringReader(
                                  "<PRE STYLE=\"background: #ffffff; border: 1px solid #000000; padding: 0.04in; line-height: 100%\"><FONT FACE=\"monospace\"><FONT SIZE=3><FONT COLOR=\"#000080\"><B><SPAN STYLE=\"background: #ffffff\">package</SPAN></B></FONT><SPAN STYLE=\"background: #ffffff\"> org.gradle.webservice;</SPAN></FONT></FONT><FONT FACE=\"monospace\"><FONT SIZE=3><FONT COLOR=\"#000080\"><B><SPAN STYLE=\"background: #ffffff\"><BR/>import</SPAN></B></FONT><SPAN STYLE=\"background: #ffffff\"> org.apache.commons.io.FilenameUtils;</SPAN></FONT></FONT><FONT FACE=\"monospace\"><FONT SIZE=3><FONT COLOR=\"#000080\"><B><SPAN STYLE=\"background: #ffffff\">import</SPAN></B></FONT><SPAN STYLE=\"background: #ffffff\"> org.apache.commons.lang.builder.ToStringBuilder;</SPAN></FONT></FONT><FONT FACE=\"monospace\"><FONT SIZE=3><FONT COLOR=\"#000080\"><B><SPAN STYLE=\"background: #ffffff\">import</SPAN></B></FONT><SPAN STYLE=\"background: #ffffff\"> org.apache.commons.collections.list.GrowthList;</SPAN></FONT></FONT><FONT FACE=\"monospace\"><FONT SIZE=3><FONT COLOR=\"#000080\"><B><SPAN STYLE=\"background: #ffffff\">import</SPAN></B></FONT><SPAN STYLE=\"background: #ffffff\"> org.gradle.shared.Person;</SPAN></FONT></FONT><FONT FACE=\"monospace\"><FONT SIZE=3><FONT COLOR=\"#000080\"><B><SPAN STYLE=\"background: #ffffff\">import</SPAN></B></FONT><SPAN STYLE=\"background: #ffffff\"> org.gradle.api.PersonList;</SPAN></FONT></FONT><FONT FACE=\"monospace\"><FONT SIZE=3><FONT COLOR=\"#000080\"><B><SPAN STYLE=\"background: #ffffff\">public</SPAN></B></FONT><SPAN STYLE=\"background: #ffffff\"> </SPAN><FONT COLOR=\"#000080\"><B><SPAN STYLE=\"background: #ffffff\">class</SPAN></B></FONT><SPAN STYLE=\"background: #ffffff\"> TestTest {</SPAN></FONT></FONT><SPAN STYLE=\"background: #ffffff\">    </SPAN><FONT COLOR=\"#000080\"><FONT FACE=\"monospace\"><FONT SIZE=3><B><SPAN STYLE=\"background: #ffffff\">private</SPAN></B></FONT></FONT></FONT><FONT FACE=\"monospace\"><FONT SIZE=3><SPAN STYLE=\"background: #ffffff\"> String name;</SPAN></FONT></FONT><SPAN STYLE=\"background: #ffffff\">    </SPAN><FONT COLOR=\"#000080\"><FONT FACE=\"monospace\"><FONT SIZE=3><B><SPAN STYLE=\"background: #ffffff\">public</SPAN></B></FONT></FONT></FONT><FONT FACE=\"monospace\"><FONT SIZE=3><SPAN STYLE=\"background: #ffffff\"> </SPAN></FONT></FONT><FONT COLOR=\"#000080\"><FONT FACE=\"monospace\"><FONT SIZE=3><B><SPAN STYLE=\"background: #ffffff\">void</SPAN></B></FONT></FONT></FONT><FONT FACE=\"monospace\"><FONT SIZE=3><SPAN STYLE=\"background: #ffffff\"> method() {</SPAN></FONT></FONT><SPAN STYLE=\"background: #ffffff\">        </SPAN><FONT FACE=\"monospace\"><FONT SIZE=3><SPAN STYLE=\"background: #ffffff\">FilenameUtils.separatorsToUnix(</SPAN></FONT></FONT><FONT COLOR=\"#008000\"><FONT FACE=\"monospace\"><FONT SIZE=3><B><SPAN STYLE=\"background: #ffffff\">&quot;my/unix/filename&quot;</SPAN></B></FONT></FONT></FONT><FONT FACE=\"monospace\"><FONT SIZE=3><SPAN STYLE=\"background: #ffffff\">);</SPAN></FONT></FONT><SPAN STYLE=\"background: #ffffff\">        </SPAN><FONT FACE=\"monospace\"><FONT SIZE=3><SPAN STYLE=\"background: #ffffff\">ToStringBuilder.reflectionToString(</SPAN></FONT></FONT><FONT COLOR=\"#000080\"><FONT FACE=\"monospace\"><FONT SIZE=3><B><SPAN STYLE=\"background: #ffffff\">new</SPAN></B></FONT></FONT></FONT><FONT FACE=\"monospace\"><FONT SIZE=3><SPAN STYLE=\"background: #ffffff\"> Person(</SPAN></FONT></FONT><FONT COLOR=\"#008000\"><FONT FACE=\"monospace\"><FONT SIZE=3><B><SPAN STYLE=\"background: #ffffff\">&quot;name&quot;</SPAN></B></FONT></FONT></FONT><FONT FACE=\"monospace\"><FONT SIZE=3><SPAN STYLE=\"background: #ffffff\">));</SPAN></FONT></FONT><SPAN STYLE=\"background: #ffffff\">        </SPAN><FONT COLOR=\"#000080\"><FONT FACE=\"monospace\"><FONT SIZE=3><B><SPAN STYLE=\"background: #ffffff\">new</SPAN></B></FONT></FONT></FONT><FONT FACE=\"monospace\"><FONT SIZE=3><SPAN STYLE=\"background: #ffffff\"> GrowthList();</SPAN></FONT></FONT><SPAN STYLE=\"background: #ffffff\">        </SPAN><FONT COLOR=\"#000080\"><FONT FACE=\"monospace\"><FONT SIZE=3><B><SPAN STYLE=\"background: #ffffff\">new</SPAN></B></FONT></FONT></FONT><FONT FACE=\"monospace\"><FONT SIZE=3><SPAN STYLE=\"background: #ffffff\"> PersonList().doSomethingWithImpl(); </SPAN></FONT></FONT><FONT COLOR=\"#808080\"><FONT FACE=\"monospace\"><FONT SIZE=3><I><SPAN STYLE=\"background: #ffffff\">// compile with api-spi, runtime with api</SPAN></I></FONT></FONT></FONT><SPAN STYLE=\"background: #ffffff\">    <FONT FACE=\"monospace\"><FONT SIZE=3>}</FONT></FONT></SPAN><FONT FACE=\"monospace\"><FONT SIZE=3><SPAN STYLE=\"background: #ffffff\">}</SPAN></FONT></FONT></PRE>");
//                                return new StringReader(text);
                              }
                              else if ("java.lang.String".equals(aClass.getName())) {
                                return text;
                              }
                              return null;
                            }
                          }, new ClipboardOwner() {
                            @Override
                            public void lostOwnership(Clipboard clipboard, Transferable contents) {

                            }
                          }
    );
  }
}
