package com.nightscout.android.settings;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.*;
import android.widget.BaseAdapter;

import com.nightscout.android.R;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* set preferences */
        addPreferencesFromResource(R.xml.preferences);

        // iterate through all preferences and update to saved value
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            initSummary(getPreferenceScreen().getPreference(i));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePrefSummary(findPreference(key));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    // iterate through all preferences and update to saved value
    private void initSummary(Preference p) {
        // PreferenceGroup covers PreferenceCategory and PreferenceScreen
        if (p instanceof PreferenceGroup) {
            PreferenceGroup group = (PreferenceGroup) p;
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                initSummary(group.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    private PreferenceScreen getParentScreen(Preference childPref) {
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            Preference p = getPreferenceScreen().getPreference(i);

            if (p instanceof PreferenceScreen) {
                PreferenceScreen screen = (PreferenceScreen)p;

                if (screen.findPreference(childPref.getKey()) != null) {
                    return screen;
                }
            }
        }

        return null;
    }

    // update preference summary
    private void updatePrefSummary(Preference p) {
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());
        }
        if (p instanceof MultiSelectListPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());
        }
        if (p instanceof CheckBoxPreference) {
            PreferenceScreen parentScreen = getParentScreen(p);

            if (parentScreen != null) {
                parentScreen.setSummary(((CheckBoxPreference)p).isChecked() ? "Enabled" : "Disabled");

                // Without this, the parent activity's PreferenceScreen item won't update when the user changes
                // the checkbox preference.
                // http://stackoverflow.com/questions/2625246/update-existing-preference-item-in-a-preferenceactivity-upon-returning-from-a-s
                ((BaseAdapter)parentScreen.getRootAdapter()).notifyDataSetChanged();
            }
        }
    }
}