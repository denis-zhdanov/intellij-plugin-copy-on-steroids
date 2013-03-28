package org.denis.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Denis Zhdanov
 * @since 3/26/13 6:40 PM
 */
@State(
  name = "CopyOnSteroidsSettings",
  storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/copy.on.steroids.xml")} 
)
public class CopyOnSteroidSettings implements PersistentStateComponent<CopyOnSteroidSettings>, ApplicationComponent {

  @NotNull public static final String ACTIVE_GLOBAL_SCHEME_MARKER = "__ACTIVE_GLOBAL_SCHEME__";

  private String  mySchemeName = ACTIVE_GLOBAL_SCHEME_MARKER;
  private boolean myProvideRtf = true;
  private boolean myStripIndents = true;
  private boolean myDebugProcessing;

  @NotNull
  public static CopyOnSteroidSettings getInstance() {
    return ApplicationManager.getApplication().getComponent(CopyOnSteroidSettings.class);
  }

  @NotNull
  public EditorColorsScheme getColorsScheme(@NotNull Editor editor) {
    EditorColorsScheme result = null;
    if (!ACTIVE_GLOBAL_SCHEME_MARKER.equals(mySchemeName)) {
      result = EditorColorsManager.getInstance().getScheme(mySchemeName);
    }
    return result == null ? editor.getColorsScheme() : result;
  }

  @Nullable
  @Override
  public CopyOnSteroidSettings getState() {
    return this;
  }

  @Override
  public void loadState(CopyOnSteroidSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  @Override
  public void initComponent() {
  }

  @Override
  public void disposeComponent() {
  }

  @NotNull
  @Override
  public String getComponentName() {
    return getClass().getName();
  }

  @NotNull
  public String getSchemeName() {
    return mySchemeName == null ? ACTIVE_GLOBAL_SCHEME_MARKER : mySchemeName;
  }

  public void setSchemeName(@Nullable String schemeName) {
    mySchemeName = schemeName;
  }

  public boolean isProvideRtf() {
    return myProvideRtf;
  }

  public void setProvideRtf(boolean provideRtf) {
    myProvideRtf = provideRtf;
  }

  public boolean isStripIndents() {
    return myStripIndents;
  }

  public void setStripIndents(boolean stripIndents) {
    myStripIndents = stripIndents;
  }

  public boolean isDebugProcessing() {
    return myDebugProcessing;
  }

  public void setDebugProcessing(boolean debugProcessing) {
    myDebugProcessing = debugProcessing;
  }
}
