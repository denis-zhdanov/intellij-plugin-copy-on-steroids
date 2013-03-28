package org.denis.settings;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.GridBag;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author Denis Zhdanov
 * @since 3/26/13 5:27 PM
 */
@SuppressWarnings("unchecked")
public class CopyOnSteroidsConfigurable extends BaseConfigurable {

  private JComponent           myContent                 = new JPanel(new GridBagLayout());
  private DefaultComboBoxModel myColorsSchemeModel       = new DefaultComboBoxModel();
  private JComboBox            myColorsSchemeComboBox    = new JComboBox(myColorsSchemeModel);
  private JBCheckBox           myStripIndentsCheckBox    = new JBCheckBox("Strip indents when appropriate");
  private JBCheckBox           myDebugProcessingCheckBox = new JBCheckBox("Debug processing");
  private JBCheckBox           myCopyRtfCheckBox         = new JBCheckBox("Provide RTF on 'Copy'");
  private JBCheckBox           myCopyHtmlCheckBox         = new JBCheckBox("Provide HTML on 'Copy'");

  @SuppressWarnings("UnusedDeclaration")
  public CopyOnSteroidsConfigurable() {

    myColorsSchemeComboBox.setRenderer(new ListCellRendererWrapper<String>() {
      @Override
      public void customize(JList list, String value, int index, boolean selected, boolean hasFocus) {
        final String textToUse;
        if (CopyOnSteroidSettings.ACTIVE_GLOBAL_SCHEME_MARKER.equals(value)) {
          textToUse = "Active scheme";
        }
        else {
          textToUse = value;
        }
        setText(textToUse);
      }
    });

    GridBagConstraints labelConstraints = new GridBag().anchor(GridBagConstraints.WEST);
    GridBagConstraints lineConstraints = new GridBag().weightx(1).coverLine().anchor(GridBagConstraints.WEST).insets(0, 7, 0, 0);

    myContent.add(new JLabel("Use colors of scheme"), labelConstraints);
    myContent.add(myColorsSchemeComboBox, lineConstraints);
    myContent.add(myStripIndentsCheckBox, lineConstraints);
    myContent.add(myDebugProcessingCheckBox, lineConstraints);
    myContent.add(myCopyRtfCheckBox, lineConstraints);
    myContent.add(myCopyHtmlCheckBox, lineConstraints);
    myContent.add(new JLabel(" "), new GridBag().weighty(1).fillCell());
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "'Copy' on Steroids";
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return myContent;
  }

  @Override
  public boolean isModified() {
    CopyOnSteroidSettings settings = CopyOnSteroidSettings.getInstance();
    return !Comparing.equal(settings.getSchemeName(), myColorsSchemeComboBox.getSelectedItem())
           || myStripIndentsCheckBox.isSelected() != settings.isStripIndents()
           || myDebugProcessingCheckBox.isSelected() != settings.isDebugProcessing()
           || myCopyRtfCheckBox.isSelected() != settings.isProvideRtf()
           || myCopyHtmlCheckBox.isSelected() != settings.isProvideHtml();
  }

  @Override
  public void apply() throws ConfigurationException {
    CopyOnSteroidSettings settings = CopyOnSteroidSettings.getInstance();
    Object item = myColorsSchemeComboBox.getSelectedItem();
    if (item instanceof String) {
      settings.setSchemeName(item.toString());
    }
    settings.setDebugProcessing(myDebugProcessingCheckBox.isSelected());
    settings.setStripIndents(myStripIndentsCheckBox.isSelected());
    settings.setProvideRtf(myCopyRtfCheckBox.isSelected());
    settings.setProvideHtml(myCopyHtmlCheckBox.isSelected());
  }

  @Override
  public void reset() {
    CopyOnSteroidSettings settings = CopyOnSteroidSettings.getInstance();
    
    myColorsSchemeModel.removeAllElements();
    EditorColorsScheme[] schemes = EditorColorsManager.getInstance().getAllSchemes();
    myColorsSchemeModel.addElement(CopyOnSteroidSettings.ACTIVE_GLOBAL_SCHEME_MARKER);
    for (EditorColorsScheme scheme : schemes) {
      myColorsSchemeModel.addElement(scheme.getName());
    }
    String toSelect = settings.getSchemeName();
    if (!StringUtil.isEmpty(toSelect)) {
      myColorsSchemeComboBox.setSelectedItem(toSelect);
    }
    
    myDebugProcessingCheckBox.setSelected(settings.isDebugProcessing());
    myStripIndentsCheckBox.setSelected(settings.isStripIndents());
    myCopyRtfCheckBox.setSelected(settings.isProvideRtf());
    myCopyHtmlCheckBox.setSelected(settings.isProvideHtml());
  }

  @Override
  public void disposeUIResources() {
    myContent = null;
    myColorsSchemeModel = null;
    myColorsSchemeComboBox = null;
    myDebugProcessingCheckBox = null; 
    myStripIndentsCheckBox = null; 
    myCopyRtfCheckBox = null;
    myCopyHtmlCheckBox = null;
  }
}
