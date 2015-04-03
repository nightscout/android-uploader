package com.nightscout.android.events;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;

import com.nightscout.android.R;
import com.nightscout.core.events.EventType;

import net.tribe7.common.base.CaseFormat;

public class UserEventPanelActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_panel);
        Intent intent = getIntent();
        EventType eventType = EventType.values()[intent.getIntExtra("Filter",
                EventType.ALL.ordinal())];
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle(String.format("%s Events",
                    CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, eventType.name())));
        }
        EventFragment fragment = new EventFragment();

        Bundle bundle = new Bundle();
        bundle.putInt("Filter", intent.getIntExtra("Filter", 0));
//        bundle.putBoolean("fromActivity", true);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.eventlogfragment, fragment);
        fragmentTransaction.commit();

    }
}
