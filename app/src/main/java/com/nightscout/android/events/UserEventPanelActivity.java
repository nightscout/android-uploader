package com.nightscout.android.events;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;

import com.google.common.base.CaseFormat;
import com.nightscout.android.R;
import com.nightscout.core.events.EventType;

public class UserEventPanelActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_panel);
        Intent intent = getIntent();
        EventType eventType = EventType.values()[intent.getIntExtra("Filter", 0)];
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle(String.format("%s Events",
                    CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, eventType.name())));
        }
        EventFragment fragment = new EventFragment();

        Bundle bundle = new Bundle();
        bundle.putInt("Filter", intent.getIntExtra("Filter", 0));
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragment, fragment);
        fragmentTransaction.commit();

    }
}
