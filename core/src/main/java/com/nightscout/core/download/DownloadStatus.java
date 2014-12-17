package com.nightscout.core.download;

/**
 * Enumeration for the state of a download
 */
public enum DownloadStatus {
  SUCCESS, // Download was successful
  NO_DATA, // Download contained no data
  DEVICE_NOT_FOUND,  // No device was found during the download
  IO_ERROR, // IO Error occurred during the download
  APPLICTION_ERROR, // Application error occurred
  NONE, // No download was performed
  UNKNOWN // Unknown event during download
}
