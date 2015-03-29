package com.nightscout.android.ui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.nightscout.android.Nightscout;
import com.nightscout.android.ProcessorService;
import com.nightscout.android.R;
import com.nightscout.android.events.UserEventPanelActivity;
import com.nightscout.android.exceptions.FeedbackDialog;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.preferences.PreferenceKeys;
import com.nightscout.core.BusProvider;
import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.drivers.AbstractDevice;
import com.nightscout.core.drivers.SupportedDevices;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.G4Download;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.model.SensorGlucoseValueEntry;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.utils.GlucoseReading;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Date;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.nightscout.core.dexcom.SpecialValue.getEGVSpecialValue;
import static com.nightscout.core.dexcom.SpecialValue.isSpecialValue;
import static org.joda.time.Duration.standardMinutes;

public class MonitorFragment extends Fragment {

    private Handler mHandler = new Handler();
    // UI components
    @InjectView(R.id.webView)
    WebView mWebView;
    @InjectView(R.id.sgValue)
    TextView mTextSGV;
    @InjectView(R.id.timeAgo)
    TextView mTextTimestamp;
    @InjectView(R.id.syncButton)
    ImageButton mSyncButton;
    @InjectView(R.id.usbButton)
    ImageButton receiverButton;
    Bus bus = BusProvider.getInstance();

    private String mJSONData;
    //    private AndroidPreferences preferences;
    private long lastRecordTime;

    @Inject
    NightscoutPreferences preferences;
    @Inject
    AppContainer appContainer;
    @Inject
    FeedbackDialog feedbackDialog;


    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        return super.onCreateView(inflater, container, savedInstanceState);
//        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.Nightscout);
//        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
        View view = inflater.inflate(R.layout.monitor_fragment, container, false);
        Nightscout app = Nightscout.get(getActivity());
        app.inject(this);
        ButterKnife.inject(this, view);
        bus.register(this);


        preferences = new AndroidPreferences(getActivity());
        mTextSGV.setTag(R.string.display_sgv, -1);
        mTextSGV.setTag(R.string.display_trend, 0);
        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setUseWideViewPort(false);
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setBackgroundColor(0);
        mWebView.loadUrl("file:///android_asset/index.html");
        // disable scroll on touch
        mWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });
        receiverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                EventFragment fragment = new EventFragment();
//
////                ((MaterialNavigationDrawer) getActivity()).setFragmentChild(new ChildFragment(),"Child Title");
//
//                FragmentTransaction transaction = getFragmentManager().beginTransaction();
//                transaction.replace(R.id.frame_container, fragment);
////                transaction.addToBackStack(null);
//                transaction.commit();
                Intent intent = new Intent(getActivity(), UserEventPanelActivity.class);
                intent.putExtra("Filter", EventType.DEVICE.ordinal());
                startActivity(intent);
            }
        });
        mSyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), UserEventPanelActivity.class);
                intent.putExtra("Filter", EventType.UPLOADER.ordinal());
                startActivity(intent);
            }
        });
        if (preferences.getDeviceType() == SupportedDevices.DEXCOM_G4_SHARE2) {
            receiverButton.setBackgroundResource(R.drawable.ic_noble);
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(PreferenceKeys.DEXCOM_DEVICE_TYPE)) {
                    int res = preferences.getDeviceType() == SupportedDevices.DEXCOM_G4 ? R.drawable.ic_nousb : R.drawable.ic_noble;
                    receiverButton.setBackgroundResource(res);
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(prefListener);

        return view;
    }

    @Override
    public void onPause() {
        mWebView.pauseTimers();
        mWebView.onPause();
        mHandler.removeCallbacks(updateTimeAgo);
        super.onPause();
    }

    @Override
    public void onResume() {
        mWebView.onResume();
        mWebView.resumeTimers();
        int sgv = (Integer) mTextSGV.getTag(R.string.display_sgv);

        int direction = (Integer) mTextSGV.getTag(R.string.display_trend);
        if (sgv != -1) {
            GlucoseReading sgvReading = new GlucoseReading(sgv, GlucoseUnit.MGDL);
            mTextSGV.setText(getSGVStringByUnit(sgvReading, TrendArrow.values()[direction]));
        }

        mWebView.loadUrl("javascript:updateUnits(" + Boolean.toString(preferences.getPreferredUnits() == GlucoseUnit.MMOL) + ")");

        mHandler.post(updateTimeAgo);

        super.onResume();
    }

    private String getSGVStringByUnit(GlucoseReading sgv, TrendArrow trend) {
        String sgvStr = sgv.asStr(preferences.getPreferredUnits());
        return (sgv.asMgdl() != -1) ?
                (isSpecialValue(sgv)) ?
                        getEGVSpecialValue(sgv).get().toString() : sgvStr + " " + trend.symbol() : "---";
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
        outState.putString("saveJSONData", mJSONData);
        outState.putString("saveTextSGV", mTextSGV.getText().toString());
        outState.putString("saveTextTimestamp", mTextTimestamp.getText().toString());
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        // Restore the state of the WebView
        mWebView.restoreState(savedInstanceState);
//        mJSONData = savedInstanceState.getString("mJSONData");
//        mTextSGV.setText(savedInstanceState.getString("saveTextSGV"));
//        mTextTimestamp.setText(savedInstanceState.getString("saveTextTimestamp"));
    }

    @Subscribe
    public void incomingData(final AbstractDevice.DeviceConnectionStatus status) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (status.connected) {
                    if (status.deviceType == SupportedDevices.DEXCOM_G4_SHARE2) {
                        receiverButton.setBackgroundResource(R.drawable.ic_ble);
                    } else if (status.deviceType == SupportedDevices.DEXCOM_G4) {
                        receiverButton.setBackgroundResource(R.drawable.ic_usb);
                    }
                } else {
                    if (status.deviceType == SupportedDevices.DEXCOM_G4_SHARE2) {
                        receiverButton.setBackgroundResource(R.drawable.ic_noble);
                    } else if (status.deviceType == SupportedDevices.DEXCOM_G4) {
                        receiverButton.setBackgroundResource(R.drawable.ic_nousb);
                    }
                }
                if (status.active) {
                    receiverButton.setBackgroundResource(R.drawable.ble_read);
                    AnimationDrawable frameAnimation = (AnimationDrawable) receiverButton.getBackground();
                    frameAnimation.start();
                }
            }
        });
    }

    @Subscribe
    public void incomingData(final ProcessorService.ProcessorResponse status) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("XXX", "Incoming status: " + status.success);
                if (status.success) {
                    mSyncButton.setBackgroundResource(R.drawable.ic_cloud);
                } else {
                    mSyncButton.setBackgroundResource(R.drawable.ic_nocloud);
                }
            }
        });
    }

    @Subscribe
    public void incomingData(final G4Download download) {
        if (download == null) {
            Log.w("XXX", "Download is NULL");
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long refTime = DateTime.parse(download.download_timestamp).getMillis();
                lastRecordTime = new DateTime().getMillis() - Duration.standardSeconds(download.receiver_system_time_sec - download.sgv.get(download.sgv.size() - 1).sys_timestamp_sec).getMillis();
                EGVRecord recentRecord = null;
                if (download.sgv.size() > 0) {
                    recentRecord = new EGVRecord(download.sgv.get(download.sgv.size() - 1), download.receiver_system_time_sec, refTime);
                    // Reload d3 chart with new data
                    JSONArray array = new JSONArray();
                    for (SensorGlucoseValueEntry sgve : download.sgv) {
                        try {
                            EGVRecord record = new EGVRecord(sgve, sgve.sys_timestamp_sec, refTime);
                            array.put(record.toJSON());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    mWebView.loadUrl("javascript:updateData(" + array + ")");
                    mTextSGV.setText(getSGVStringByUnit(recentRecord.getReading(), recentRecord.getTrend()));
                    mTextSGV.setTag(R.string.display_sgv, recentRecord.getReading().asMgdl());
                    mTextSGV.setTag(R.string.display_trend, recentRecord.getTrend().ordinal());
                }

                // Update UI with latest record information

                EGVRecord lastEgvRecord = new EGVRecord(download.sgv.get(download.sgv.size() - 1), download.receiver_system_time_sec, refTime);
                String timeAgoStr = "---";
                if (lastEgvRecord.getRawSystemTimeSeconds() > 0) {
                    timeAgoStr = Utils.getTimeString(download.receiver_system_time_sec - lastEgvRecord.getRawSystemTimeSeconds());
                }

                mTextTimestamp.setText(timeAgoStr);
                mTextTimestamp.setTag(timeAgoStr);
            }
        });
    }

    public Runnable updateTimeAgo = new Runnable() {
        @Override
        public void run() {
            // FIXME: doesn't calculate timeago properly.
            long delta = new Date().getTime() - lastRecordTime;
            if (lastRecordTime == 0) delta = 0;

            String timeAgoStr;
            if (lastRecordTime == -1) {
                timeAgoStr = "---";
            } else if (delta < 0) {
                timeAgoStr = getString(R.string.TimeChangeDetected);
            } else {
                timeAgoStr = Utils.getTimeString(delta);
            }
            mTextTimestamp.setText(timeAgoStr);
            mHandler.removeCallbacks(updateTimeAgo);
            long delay = delta % standardMinutes(1).getMillis();
            mHandler.postDelayed(updateTimeAgo, delay);
        }
    };

}
