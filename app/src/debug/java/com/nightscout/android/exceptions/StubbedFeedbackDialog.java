package com.nightscout.android.exceptions;

import android.app.Application;
import android.widget.Toast;

public class StubbedFeedbackDialog implements FeedbackDialog {
    private final Application application;

    public StubbedFeedbackDialog(Application app) {
        this.application = app;
    }

    @Override
    public void show() {
        // TODO(trhodeos): make this actually a dialog.
        Toast.makeText(application, "Debug mode: stubbed feedback report.", Toast.LENGTH_LONG)
                .show();
    }
}
