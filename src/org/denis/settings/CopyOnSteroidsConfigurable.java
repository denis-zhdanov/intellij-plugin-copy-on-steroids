package org.denis.settings;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.util.ui.GridBag;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author Denis Zhdanov
 * @since 3/26/13 5:27 PM
 */
@SuppressWarnings("unchecked")
public class CopyOnSteroidsConfigurable extends BaseConfigurable {

  @NotNull private JComponent           myContent              = new JPanel(new GridBagLayout());
  @NotNull private DefaultComboBoxModel myColorsSchemeModel    = new DefaultComboBoxModel();
  @NotNull private JComboBox            myColorsSchemeComboBox = new JComboBox(myColorsSchemeModel);

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
    
    GridBagConstraints lineConstraints = new GridBag().weightx(1).fillCell().coverLine().anchor(GridBagConstraints.WEST).insets(0, 7, 0, 0);
    GridBagConstraints labelConstraints = new GridBag().anchor(GridBagConstraints.WEST);

    myContent.add(new JLabel("Use colors of scheme"), labelConstraints);
    myContent.add(myColorsSchemeComboBox, lineConstraints);
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
  public void apply() throws ConfigurationException {
    // TODO den implement 
  }

  @Override
  public void reset() {
    EditorColorsScheme[] schemes = EditorColorsManager.getInstance().getAllSchemes();
    myColorsSchemeModel.addElement(CopyOnSteroidSettings.ACTIVE_GLOBAL_SCHEME_MARKER);
    for (EditorColorsScheme scheme : schemes) {
      myColorsSchemeModel.addElement(scheme.getName());
    }

    String toSelect = CopyOnSteroidSettings.getInstance().getSchemeName();
    if (!StringUtil.isEmpty(toSelect)) {
      myColorsSchemeComboBox.setSelectedItem(toSelect);
    }
  }

  @Override
  public void disposeUIResources() {
    // TODO den implement 
  }
}
