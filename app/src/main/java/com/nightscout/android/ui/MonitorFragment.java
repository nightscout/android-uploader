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
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.drivers.DeviceConnectionStatus;
import com.nightscout.core.drivers.DeviceType;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.GlucoseUnit;
import com.nightscout.core.model.v2.Download;
import com.nightscout.core.model.v2.DownloadStatus;
import com.nightscout.core.model.v2.G4Data;
import com.nightscout.core.model.v2.SensorGlucoseValue;
import com.nightscout.core.model.v2.converters.SensorGlucoseValueConverter;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.utils.DexcomG4Utils;
import com.nightscout.core.utils.ListUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.wire.Wire;

import net.tribe7.common.base.Optional;
import net.tribe7.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

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
    private SensorGlucoseValue latestRecord;

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
        log.debug("onCreateView called");
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
      int res = preferences.getDeviceType() == DeviceType.DEXCOM_G4 ? R.drawable.ic_nousb : R.drawable.ic_noble;
      setReceiverButtonRes(res);
    }

    private void refreshUnits() {
      mWebView.loadUrl("javascript:updateUnits(" + Boolean.toString(preferences.getPreferredUnits() == GlucoseUnit.MMOL) + ")");
      restoreSgvText();
    }

    private Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
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
            //setReceiverButtonRes(getReceiverRes(mCollectorService.getDeviceConnectionStatus()));
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

    private void setSgvText(SensorGlucoseValue sensorGlucoseValue) {
        String text = "---";
        if (sensorGlucoseValue.glucose_mgdl != -1) {
            text = DexcomG4Utils.getDisplayableGlucoseValueString(sensorGlucoseValue, preferences.getPreferredUnits());
        }
        mTextSGV.setText(text);
    }

    private void restoreSgvText() {
        setSgvText(latestRecord);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
        outState.putString("saveTextSGV", mTextSGV.getText().toString());
        outState.putString("saveTextTimestamp", mTextTimestamp.getText().toString());
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
        log.debug(
            "Device Type: " + status.deviceType + " Device State: " + status.deviceState.name());
        switch (status.deviceState) {
            case CONNECTED:
                res = (status.deviceType == DeviceType.DEXCOM_G4) ? R.drawable.ic_usb : R.drawable.ic_ble;
                break;
            case CLOSED:
                res = (status.deviceType == DeviceType.DEXCOM_G4) ? R.drawable.ic_nousb : R.drawable.ic_noble;
                break;
            case READING:
            case WRITING:
            case CONNECTING:
                res = (status.deviceType == DeviceType.DEXCOM_G4) ? R.drawable.ic_usb : R.drawable.ble_read;
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
    public void incomingData(final Download uiDownload) {
        if (getActivity() == null) {
            log.info("Activity is null!");
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (uiDownload.status != DownloadStatus.SUCCESS) {
                    log.info("Unsuccessful download encountered (status: {}). Skipping.", uiDownload.status.name());
                    return;
                }
                if (uiDownload.g4_data == null) {
                    log.error("Successful download with null G4Data encountered. Skipping!");
                    return;
                }
                G4Data g4Data = uiDownload.g4_data;
                Optional<SensorGlucoseValue>
                    sensorGlucoseValueOptional = ListUtils.lastOrEmpty(g4Data.sensor_glucose_values);
                if (!sensorGlucoseValueOptional.isPresent()) {
                    log.debug("Move along. Nothing to see here. No data that I'm interested in the download");
                    return;
                }
                JSONArray array = new JSONArray(Lists.transform(Wire.get(
                                                                    g4Data.sensor_glucose_values,
                                                                    G4Data.DEFAULT_SENSOR_GLUCOSE_VALUES),
                    SensorGlucoseValueConverter
                        .instance()));
                mWebView.loadUrl("javascript:updateData(" + array + ")");

                SensorGlucoseValue recentRecord = sensorGlucoseValueOptional.get();
                setSgvText(recentRecord);
                latestRecord = recentRecord;

                String timeAgoStr = "---";

                if (latestRecord.timestamp.system_time_sec > 0) {
                    timeAgoStr = Utils.getTimeString(g4Data.receiver_system_time_sec - latestRecord.timestamp.system_time_sec);
                }

                mTextTimestamp.setText(timeAgoStr);
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
            DateTime now = new DateTime();
            DateTime lastReading = Utils.receiverTimeToDateTime(latestRecord);
            String timeAgoString;
            if (lastReading == null) {
                timeAgoString = "---";
            } else if (now.isBefore(lastReading)) {
                timeAgoString = getString(R.string.TimeChangeDetected);
            } else {
                timeAgoString = Utils.getTimeString(DexcomG4Utils.timeSinceReading(now, latestRecord).toDurationMillis());
            }
            mTextTimestamp.setText(timeAgoString);
            mHandler.removeCallbacks(updateTimeAgo);
            mHandler.postDelayed(updateTimeAgo, standardMinutes(1).getMillis());
        }
    };

}
