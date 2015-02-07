package com.nightscout.android.exceptions;

import com.nightscout.android.R;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ACRAConfigurationException;
import org.acra.ReportingInteractionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcraFeedbackDialog implements FeedbackDialog {

  private static final Logger log = LoggerFactory.getLogger(AcraFeedbackDialog.class);

  @Override
  public void show() {
    ACRAConfiguration acraConfiguration = ACRA.getConfig();
    // Set to dialog to get user comments
    try {
      acraConfiguration.setMode(ReportingInteractionMode.DIALOG);
      acraConfiguration.setResToastText(0);
    } catch (ACRAConfigurationException e) {
      log.error(e.getMessage());
    }
    ACRA.getErrorReporter().handleException(null);
    // Reset back to toast
    try {
      acraConfiguration.setResToastText(R.string.crash_toast_text);
      acraConfiguration.setMode(ReportingInteractionMode.TOAST);
    } catch (ACRAConfigurationException e) {
      log.error(e.getMessage());
    }
  }
}
