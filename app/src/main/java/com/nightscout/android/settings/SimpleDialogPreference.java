package com.nightscout.android.settings;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

/**
 * Preference item that open a simple dialog on click. Performs no actions. Mainly for displaying
 * information.
 */
public class SimpleDialogPreference extends DialogPreference {

    public SimpleDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}
