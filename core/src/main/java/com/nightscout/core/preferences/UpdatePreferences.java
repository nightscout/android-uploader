package com.nightscout.core.preferences;

/**
 * Preferences have been updated for the given key.
 */
public class UpdatePreferences {

  private final String key;

  public UpdatePreferences(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
