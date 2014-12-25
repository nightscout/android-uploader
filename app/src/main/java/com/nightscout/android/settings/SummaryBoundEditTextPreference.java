package com.nightscout.android.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class SummaryBoundEditTextPreference extends EditTextPreference {

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public SummaryBoundEditTextPreference(Context context,
                                        AttributeSet attrs,
                                        int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public SummaryBoundEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public SummaryBoundEditTextPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SummaryBoundEditTextPreference(Context context) {
    super(context);
  }

  @Override
  public void setText(String text) {
    super.setText(text);
    setSummary(text);
  }
}
