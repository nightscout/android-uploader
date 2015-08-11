package com.nightscout.core.upload.v2;

import com.nightscout.core.model.v2.Download;
import com.nightscout.core.model.v2.DownloadStatus;
import com.nightscout.core.model.v2.G4Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
   */
  protected abstract void handleG4Data(final G4Data download);

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
    if (!isEnabled() || download == null || download.status != DownloadStatus.SUCCESS || download.g4_data == null) {
      return;
    }
    log.debug("Calling handleG4Data.");
    handleG4Data(download.g4_data);
  }
}
