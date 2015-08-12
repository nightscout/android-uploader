package com.nightscout.core.upload.v2;

import com.nightscout.core.model.v2.Download;
import com.nightscout.core.model.v2.DownloadStatus;
import com.nightscout.core.model.v2.G4Data;
import com.squareup.wire.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import rx.Subscriber;

public abstract class G4DataHandler extends Subscriber<Download> {

  private static final Logger log = LoggerFactory.getLogger(G4DataHandler.class);
  /**
   * Whether this upload handler is currently enabled.
   * @return true if enabled, false otherwise.
   */
  protected abstract boolean isEnabled();

  /**
   * Handle the given g4 data.
   * @param download non-null g4 data to upload.
   * @return list of messages that were successfully handled.
   */
  protected abstract List<Message> handleG4Data(final G4Data download);

  /**
   * Handles a settings refresh. Note that this could be called on any thread. 
   */
  protected void handleSettingsRefresh() {
    // Empty.
  }

  @Override
  public void onCompleted() {
    log.debug("onCompleted called.");
  }

  @Override
  public void onError(Throwable e) {
    log.error("Error encountered: ", e);
  }

  @Override
  public void onNext(Download download) {
    if (!isEnabled()) {
      log.info("Uploader not enabled. Skipping.");
      return;
    }
    if (download == null) {
      log.warn("Download was null. This shouldn't happen! Skipping.");
      return;
    }
    if (download.status != DownloadStatus.SUCCESS) {
      log.warn("Unsuccessful download encountered. This shouldn't happen! Skipping");
      return;
    }
    if (download.g4_data == null) {
      log.warn("Null g4 data encountered. This shouldn't happen! Skipping.");
      return;
    }
    log.debug("Calling handleG4Data.");
    handleG4Data(download.g4_data);
  }
}
