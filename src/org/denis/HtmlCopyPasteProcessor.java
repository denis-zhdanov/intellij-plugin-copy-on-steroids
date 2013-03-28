package org.denis;

import org.denis.model.SyntaxInfo;
import org.denis.settings.CopyOnSteroidSettings;
import org.denis.view.HtmlTransferableData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Denis Zhdanov
 * @since 3/28/13 1:05 PM
 */
public class HtmlCopyPasteProcessor extends AbstractCopyPasteSyntaxAwareProcessor<HtmlTransferableData> {

  @Nullable
  @Override
  protected HtmlTransferableData build(@NotNull SyntaxInfo info) {
    return new HtmlTransferableData(info);
  }

  @Override
  protected boolean isEnabled(@NotNull CopyOnSteroidSettings settings) {
    return settings.isProvideHtml();
  }
}
