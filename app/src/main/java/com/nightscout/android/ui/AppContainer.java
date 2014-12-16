package com.nightscout.android.ui;

import android.app.Activity;
import android.view.ViewGroup;

import static butterknife.ButterKnife.findById;

/**
 * An indirection which allows controlling the root container used for each activity.
 */
public interface AppContainer {

  /**
   * The root {@link android.view.ViewGroup} into which the activity should place its contents.
   */
  ViewGroup get(Activity activity);

  final AppContainer DEFAULT = new AppContainer() {
    @Override
    public ViewGroup get(Activity activity) {
      return findById(activity, android.R.id.content);
    }
  };
}
