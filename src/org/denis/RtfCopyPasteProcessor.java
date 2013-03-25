package org.denis;

import org.denis.model.SyntaxInfo;
import org.denis.view.RtfTransferableData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Denis Zhdanov
 * @since 3/25/13 2:18 PM
 */
public class RtfCopyPasteProcessor extends AbstractCopyPasteSyntaxAwareProcessor<RtfTransferableData> {

  @Nullable
  @Override
  protected RtfTransferableData build(@NotNull SyntaxInfo info) {
    return new RtfTransferableData(info);
  }
}
