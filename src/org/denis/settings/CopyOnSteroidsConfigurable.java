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

  private JComponent           myContent              = new JPanel(new GridBagLayout());
  private DefaultComboBoxModel myColorsSchemeModel    = new DefaultComboBoxModel();
  private JComboBox            myColorsSchemeComboBox = new JComboBox(myColorsSchemeModel);
  private JBCheckBox           myCopyRtfCheckBox      = new JBCheckBox("Provide RTF on 'Copy'");

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
    myContent.add(myCopyRtfCheckBox, lineConstraints);
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

  @SuppressWarnings("SimplifiableIfStatement")
  @Override
  public boolean isModified() {
    CopyOnSteroidSettings settings = CopyOnSteroidSettings.getInstance();
    if (!Comparing.equal(settings.getSchemeName(), myColorsSchemeComboBox.getSelectedItem())) {
      return false;
    }
    return myCopyRtfCheckBox.isSelected() != settings.isProvideRtf();
  }

  @Override
  public void apply() throws ConfigurationException {
    CopyOnSteroidSettings settings = CopyOnSteroidSettings.getInstance();
    Object item = myColorsSchemeComboBox.getSelectedItem();
    if (item instanceof String) {
      settings.setSchemeName(item.toString());
    }
    settings.setProvideRtf(myCopyRtfCheckBox.isSelected());
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
    
    myCopyRtfCheckBox.setSelected(settings.isProvideRtf());
  }

  @Override
  public void disposeUIResources() {
    myContent = null;
    myColorsSchemeModel = null;
    myColorsSchemeComboBox = null;
    myCopyRtfCheckBox = null; 
  }
}
