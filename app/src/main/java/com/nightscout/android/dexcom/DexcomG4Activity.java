package com.nightscout.android.dexcom;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nightscout.android.BuildConfig;
import com.nightscout.android.R;
import com.nightscout.android.settings.SettingsActivity;
import com.nightscout.android.upload.UploadHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/* Main activity for the DexcomG4Activity program */
public class DexcomG4Activity extends Activity {

    private static final String TAG = DexcomG4Activity.class.getSimpleName();
    private static final int REQUEST_CODE_SETTINGS = 1;

    private Handler mHandler = new Handler();

    private int maxRetries = 20;
    private int retryCount = 0;

    private TextView mTitleTextView;
    private TextView mDumpTextView;
    private Button b1;


    //All I'm really doing here is creating a simple activity to launch and maintain the service
    private Runnable updateDataView = new Runnable() {
        public void run() {
        if (!isMyServiceRunning()) {
            if (retryCount < maxRetries) {
                startService(new Intent(DexcomG4Activity.this, DexcomG4Service.class));
                mTitleTextView.setTextColor(Color.YELLOW);
                mTitleTextView.setText("Connecting...");
                Log.i(TAG, "Starting service " + retryCount + "/" + maxRetries);
                ++retryCount;
            } else {
                mHandler.removeCallbacks(updateDataView);
                Log.i(TAG, "Unable to restart service, trying to recreate the activity");
                recreate();
            }
        } else {
            mTitleTextView.setTextColor(Color.GREEN);
            mTitleTextView.setText("CGM Service Started");
            EGVRecord record = DexcomG4Activity.this.loadClassFile(new File(getBaseContext().getFilesDir(), "save.bin"));
            mDumpTextView.setTextColor(Color.WHITE);
            mDumpTextView.setText("\n" + record.displayTime + "\n" + record.bGValue + "\n" + record.trendArrow + "\n");
        }
        mHandler.postDelayed(updateDataView, 30000);
        }
    };

    //Look for and launch the service, display status to user
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.adb);

        mTitleTextView = (TextView) findViewById(R.id.demoTitle);
        mDumpTextView = (TextView) findViewById(R.id.demoText);

        LinearLayout lnr = (LinearLayout) findViewById(R.id.container);

        b1 = new Button(this);

        mHandler.post(updateDataView);

        mTitleTextView.setTextColor(Color.YELLOW);
        mTitleTextView.setText("CGM Service Pending");

        b1.setText("Stop Uploading CGM Data");
        lnr.addView(b1);

        b1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            if (b1.getText() == "Stop Uploading CGM Data") {
                mHandler.removeCallbacks(updateDataView);
                stopService(new Intent(DexcomG4Activity.this, DexcomG4Service.class));
                b1.setText("Start Uploading CGM Data");
                mTitleTextView.setTextColor(Color.RED);
                mTitleTextView.setText("CGM Service Stopped");
                finish();

            } else {
                mHandler.removeCallbacks(updateDataView);
                mHandler.post(updateDataView);
                b1.setText("Stop Uploading CGM Data");
            }
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Refresh the status
        EGVRecord record = this.loadClassFile(new File(this.getFilesDir(), "save.bin"));
        mDumpTextView.setTextColor(Color.WHITE);
        mDumpTextView.setText("\n" + record.displayTime + "\n" + record.bGValue + "\n" + record.trendArrow + "\n");
    }

    //Check to see if service is running
    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DexcomG4Service.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    //Deserialize the EGVRecord (most recent) value
    public EGVRecord loadClassFile(File f) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            Object o = ois.readObject();
            return (EGVRecord) o;
        } catch (Exception ex) {
            Log.w(TAG, " unable to loadEGVRecord");
        }
        return new EGVRecord();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_CODE_SETTINGS) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

            if (prefs.getBoolean("EnablePushover", false)) {
                new AlertDialog.Builder(this)
                    .setTitle("Push Notifications Enabled")
                    .setMessage("Would you like to send a test notification now to confirm that Pushover is configured correctly?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            try {
                                UploadHelper uploader = new UploadHelper(getBaseContext());

                                EGVRecord record = new EGVRecord();
                                record.setBGValue("14"); // Test notification
                                record.setDisplayTime(new SimpleDateFormat("MM/dd/yyy hh:mm:ss aa").format(new Date()));

                                uploader.execute(record);
                            } catch (Exception ex) {
                                new AlertDialog.Builder(getBaseContext())
                                    .setMessage("An error occurred sending test notification.")
                                    .show();
                            }
                        }})
                    .setNegativeButton(android.R.string.no, null).show();
            }
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        if (BuildConfig.DEBUG) {
            menu.findItem(R.id.menu_triggerhigh).setVisible(true);
            menu.findItem(R.id.menu_triggerlow).setVisible(true);
            menu.findItem(R.id.menu_triggerquestion).setVisible(true);
            menu.findItem(R.id.menu_triggerhourglass).setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SETTINGS);
                break;
            case R.id.menu_triggerhigh:
                triggerReading(200);
                break;
            case R.id.menu_triggerlow:
                triggerReading(60);
                break;
            case R.id.menu_triggerquestion:
                triggerReading(10);
                break;
            case R.id.menu_triggerhourglass:
                triggerReading(9);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void triggerReading(int bGV) {
        UploadHelper uploader = new UploadHelper(getBaseContext());

        EGVRecord record = new EGVRecord();
        record.setBGValue(String.valueOf(bGV));
        record.setDisplayTime(new SimpleDateFormat("MM/dd/yyy hh:mm:ss aa").format(new Date()));
        record.setTrend("SingleUp");
        record.setTrendArrow("↑");

        uploader.execute(record);
    }
}
