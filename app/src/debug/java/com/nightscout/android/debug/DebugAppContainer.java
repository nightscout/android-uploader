package com.nightscout.android.debug;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.nightscout.android.R;
import com.nightscout.android.SyncingService;
import com.nightscout.android.ui.AppContainer;
import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.model.G4Noise;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static butterknife.ButterKnife.findById;
import static org.joda.time.Duration.standardMinutes;

/**
 * An {@link AppContainer} for debug builds which wrap the content view with a sliding drawer on
 * the right that holds all of the debug information and settings.
 */
@Singleton
public class DebugAppContainer implements AppContainer {

  private final Application application;

  Activity activity;
  Context drawerContext;

  @Inject
  public DebugAppContainer(Application application) {
    this.application = application;
  }

  @InjectView(R.id.debug_drawer_layout) DrawerLayout drawerLayout;
  @InjectView(R.id.debug_content) LinearLayout content;
  @InjectView(R.id.debug_egv_input) EditText egvInput;
  @InjectView(R.id.debug_generate_egv_button) Button generateEgvButton;


  @Override
  public ViewGroup get(final Activity activity) {
    this.activity = activity;
    drawerContext = activity;

    activity.setContentView(R.layout.debug_activity_frame);

    // Manually find the debug drawer and inflate the drawer layout inside of it.
    ViewGroup drawer = findById(activity, R.id.debug_drawer);
    LayoutInflater.from(drawerContext).inflate(R.layout.debug_drawer_content, drawer);

    // Inject after inflating the drawer layout so its views are available to inject.
    ButterKnife.inject(this, activity);

    generateEgvButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        int input = Integer.parseInt(egvInput.getText().toString());
        EGVRecord record = new EGVRecord(input, TrendArrow.NONE, new Date(), new Date(), G4Noise.NOISE_NONE);
        Toast.makeText(application.getApplicationContext(), "TODO: make this do interesting things", Toast.LENGTH_SHORT).show();
      }
    });

    return content;
  }
}
