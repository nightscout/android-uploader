package com.nightscout.android.ui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nightscout.android.CollectorService;
import com.nightscout.android.Nightscout;
import com.nightscout.android.ProcessorService;
import com.nightscout.android.R;
import com.nightscout.android.events.UserEventPanelActivity;
import com.nightscout.core.BusProvider;
import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.drivers.DeviceConnectionStatus;
import com.nightscout.core.drivers.DexcomG4;
import com.nightscout.core.drivers.SupportedDevices;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.model.SensorGlucoseValueEntry;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.utils.GlucoseReading;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Minutes;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.nightscout.core.dexcom.SpecialValue.getEGVSpecialValue;
import static com.nightscout.core.dexcom.SpecialValue.isSpecialValue;
import static org.joda.time.Duration.standardMinutes;

public class MonitorFragment extends Fragment {
    protected static final Logger log = LoggerFactory.getLogger(MonitorFragment.class);

    private Handler mHandler = new Handler();
    // UI components
    @InjectView(R.id.webView)
    WebView mWebView;
    @InjectView(R.id.sgValue)
    TextView mTextSGV;
    @InjectView(R.id.timeAgo)
    TextView mTextTimestamp;
    @InjectView(R.id.syncButton)
    ImageButton uploadButton;
    @InjectView(R.id.usbButton)
    ImageButton receiverButton;

    @InjectView(R.id.circularProgressbar)
    ProgressBar progressBar;
    private Bus bus = BusProvider.getInstance();

    private long lastRecordTime;

    @Inject
    NightscoutPreferences preferences;

    private CollectorService mCollectorService;
    private ProcessorService mProcessorService;
    private boolean mBound = false;

    private ServiceConnection mCollectorConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CollectorService.LocalBinder binder = (CollectorService.LocalBinder) service;
            mCollectorService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private ServiceConnection mProcessorConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ProcessorService.LocalBinder binder = (ProcessorService.LocalBinder) service;
            mProcessorService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.debug("onCreate called");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        log.debug("onActivityCreate called");
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log.info("onCreateView called");
        View view = inflater.inflate(R.layout.monitor_fragment, container, false);
        Nightscout app = Nightscout.get(getActivity());
        app.inject(this);
        ButterKnife.inject(this, view);
        bus.register(this);


        progressBar.setMax((int) Minutes.minutes(5).toStandardDuration().getMillis());
        progressBar.setProgress(0);
        mTextSGV.setTag(R.string.display_sgv, -1);
        mTextSGV.setTag(R.string.display_trend, preferences.getPreferredUnits().ordinal());
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

                Intent intent = new Intent(getActivity(), UserEventPanelActivity.class);
                intent.putExtra("Filter", EventType.DEVICE.ordinal());
                startActivity(intent);
            }
        });
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), UserEventPanelActivity.class);
                intent.putExtra("Filter", EventType.UPLOADER.ordinal());
                startActivity(intent);
            }
        });

        refreshSettingBoundUis();

        mHandler.post(updateProgress);
        return view;
    }

    private void refreshSettingBoundUis() {
      refreshDeviceType();
      refreshUnits();
    }

    private void refreshDeviceType() {
      int res = preferences.getDeviceType() == SupportedDevices.DEXCOM_G4 ? R.drawable.ic_nousb : R.drawable.ic_noble;
      setReceiverButtonRes(res);
    }

    private void refreshUnits() {
      mWebView.loadUrl("javascript:updateUnits(" + Boolean.toString(preferences.getPreferredUnits() == GlucoseUnit.MMOL) + ")");
      restoreSgvText();
    }

    private Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            log.debug("Updating progress bar");
            Intent intent = new Intent(getActivity(), CollectorService.class);
            getActivity().bindService(intent, mCollectorConnection, Context.BIND_AUTO_CREATE);
            if (mBound) {
                int progress = (int) mCollectorService.getNextPoll();
                progressBar.setProgress(progress);
                getActivity().unbindService(mCollectorConnection);
            }
            mHandler.removeCallbacks(updateProgress);
            mHandler.postDelayed(updateProgress, 2000);
        }
    };

    @Override
    public void onPause() {
        mWebView.pauseTimers();
        mWebView.onPause();
        mHandler.removeCallbacks(updateTimeAgo);
        mHandler.removeCallbacks(updateProgress);
        super.onPause();
    }

    @Override
    public void onResume() {
        log.info("onResume called");
        mWebView.onResume();
        mWebView.resumeTimers();
        Intent intent = new Intent(this.getActivity(), CollectorService.class);
        getActivity().bindService(intent, mCollectorConnection, Context.BIND_AUTO_CREATE);
        if (mBound) {
            setReceiverButtonRes(getReceiverRes(mCollectorService.getDeviceConnectionStatus()));
            getActivity().unbindService(mCollectorConnection);
        }
        intent = new Intent(this.getActivity(), ProcessorService.class);
        getActivity().bindService(intent, mProcessorConnection, Context.BIND_AUTO_CREATE);
        if (mBound) {
            int lastUpload = mProcessorService.getLastUploadStatus();
            log.warn("Last upload status: {}", (lastUpload == 1) ? "Success" : "Failed");
            setUploaderButtonRes(getUploaderRes(lastUpload));
            getActivity().unbindService(mProcessorConnection);

        }

        mHandler.post(updateTimeAgo);
        mHandler.post(updateProgress);
        refreshSettingBoundUis();
        super.onResume();
    }

    private void setSgvText(GlucoseReading sgv, TrendArrow trend) {
        String text = "---";
        if (sgv.asMgdl() != -1) {
            text = isSpecialValue(sgv) ? getEGVSpecialValue(sgv).get().toString() : sgv.asStr(preferences.getPreferredUnits()) + trend.symbol();
        }
        mTextSGV.setText(text);
        mTextSGV.setTag(R.string.display_sgv, sgv.asMgdl());
        mTextSGV.setTag(R.string.display_trend, trend.ordinal());
    }

    private void restoreSgvText() {
        GlucoseReading reading = new GlucoseReading((int) mTextSGV.getTag(R.string.display_sgv), GlucoseUnit.MGDL);
        setSgvText(reading, TrendArrow.values()[(int) mTextSGV.getTag(R.string.display_trend)]);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
        outState.putString("saveTextSGV", mTextSGV.getText().toString());
        outState.putString("saveTextTimestamp", mTextTimestamp.getText().toString());
        if (receiverButton.getTag() != null) {
            log.info("Saving device state");
            outState.putInt("deviceState", (int) receiverButton.getTag());
        }
        if (uploadButton.getTag() != null) {
            log.info("Saving sync state");
            outState.putInt("syncState", (int) uploadButton.getTag());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        // Restore the state of the WebView
        mWebView.restoreState(savedInstanceState);
    }

    @Subscribe
    public void incomingData(final DeviceConnectionStatus status) {
        if (getActivity() == null) {
            log.error("Activity is null!");
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setReceiverButtonRes(getReceiverRes(status));
            }
        });

    }

    private int getReceiverRes(DeviceConnectionStatus status) {
        int res = 0;
        log.info("Device Type: " + status.deviceType + " Device State: " + status.deviceState.name());
        switch (status.deviceState) {
            case CONNECTED:
                res = (status.deviceType == SupportedDevices.DEXCOM_G4) ? R.drawable.ic_usb : R.drawable.ic_ble;
                break;
            case CLOSED:
                res = (status.deviceType == SupportedDevices.DEXCOM_G4) ? R.drawable.ic_nousb : R.drawable.ic_noble;
                break;
            case READING:
            case WRITING:
            case CONNECTING:
                res = (status.deviceType == SupportedDevices.DEXCOM_G4) ? R.drawable.ic_usb : R.drawable.ble_read;
                break;
        }
        return res;
    }

    private void setReceiverButtonRes(int res) {
        receiverButton.setBackgroundResource(res);
        receiverButton.setTag(res);
        if (res == R.drawable.ble_read) {
            AnimationDrawable frameAnimation = (AnimationDrawable) receiverButton.getBackground();
            frameAnimation.start();
        }
    }

    private void setUploaderButtonRes(int res) {
        uploadButton.setBackgroundResource(res);
        uploadButton.setTag(res);
    }

    private int getUploaderRes(int status) {
        if (status == 0) {
            return R.drawable.ic_idlecloud;
        } else if (status == 1) {
            return R.drawable.ic_cloud;
        } else if (status == 2) {
            return R.drawable.ic_nocloud;
        }
        return R.drawable.ic_nocloud;
    }

    @Subscribe
    public void incomingData(final ProcessorService.ProcessorResponse status) {
        if (getActivity() == null) {
            log.info("Activity is null for ProcessorResponse");
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log.info("Incoming status: " + status.success);
                int stat = (status.success) ? 1 : 2;
                setUploaderButtonRes(getUploaderRes(stat));

            }
        });
    }


    @Subscribe
    public void incomingData(final DexcomG4.UIDownload uiDownload) {
        if (uiDownload.download == null) {
            log.info("Download is NULL");
            return;
        }
        if (getActivity() == null) {
            log.info("Activity is null!");
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long refTime = DateTime.parse(uiDownload.download.download_timestamp).getMillis();
                if (uiDownload.download.sgv.size() > 0) {
                    lastRecordTime = new DateTime().getMillis() - Duration.standardSeconds(uiDownload.download.receiver_system_time_sec - uiDownload.download.sgv.get(uiDownload.download.sgv.size() - 1).sys_timestamp_sec).getMillis();
                    EGVRecord recentRecord;
                    if (uiDownload.download.sgv.size() > 0) {
                        recentRecord = new EGVRecord(uiDownload.download.sgv.get(uiDownload.download.sgv.size() - 1), uiDownload.download.receiver_system_time_sec, refTime);
                        // Reload d3 chart with new data
                        JSONArray array = new JSONArray();
                        for (SensorGlucoseValueEntry sgve : uiDownload.download.sgv) {
                            try {
                                EGVRecord record = new EGVRecord(sgve, sgve.sys_timestamp_sec, refTime);
                                array.put(record.toJSON());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        mWebView.loadUrl("javascript:updateData(" + array + ")");
                        setSgvText(recentRecord.getReading(), recentRecord.getTrend());
                        // Update UI with latest record information

                        String timeAgoStr = "---";
                        if (recentRecord.getRawSystemTimeSeconds() > 0) {
                            timeAgoStr = Utils.getTimeString(uiDownload.download.receiver_system_time_sec - recentRecord.getRawSystemTimeSeconds());
                        }

                        mTextTimestamp.setText(timeAgoStr);
                        mTextTimestamp.setTag(timeAgoStr);
                    }

                } else {
                    log.debug("Move along. Nothing to see here. No data that I'm interested in in the download");
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bus.unregister(this);
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
