package com.nightscout.android;

import android.app.Activity;

public class MainActivity extends Activity {
//    private static final String TAG = MainActivity.class.getSimpleName();
//    private static final String ACTION_POLL = "com.nightscout.android.dexcom.action.POLL";
//
//    // Receivers
////    private CGMStatusReceiver mCGMStatusReceiver;
//    private ToastReceiver toastReceiver;
//
//    // Member components
//    private Handler mHandler = new Handler();
//    private String mJSONData;
//    private long lastRecordTime = -1;
////    private long receiverOffsetFromUploader = 0;
//
//    @Inject NightscoutPreferences preferences;
//    @Inject AppContainer appContainer;
//    @Inject FeedbackDialog feedbackDialog;
//
//    // Analytics mTracker
//    private Tracker mTracker;
//
//    // UI components
////    @InjectView(R.id.webView) WebView mWebView;
////    @InjectView(R.id.sgValue) TextView mTextSGV;
////    @InjectView(R.id.timeAgo) TextView mTextTimestamp;
////    @InjectView(R.id.syncButton) ImageButton mSyncButton;
////    @InjectView(R.id.usbButton) ImageButton mUsbButton;
//
//    private Pebble pebble;
//    private AndroidUploaderDevice uploaderDevice;
//    private MqttEventMgr mqttManager;
//    private AndroidEventReporter reporter;
//
//    private AlarmManager alarmManager;
//    private PendingIntent syncManager;
//
//    private Bus bus = BusProvider.getInstance();
//
//    @SuppressLint("SetJavaScriptEnabled")
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Log.d(TAG, "OnCreate called.");
//
//        Nightscout app = Nightscout.get(this);
//        app.inject(this);
//
//        bus.register(this);
//
//        ViewGroup group = appContainer.get(this);
//        getLayoutInflater().inflate(R.layout.activity_main, group);
////
////        ButterKnife.inject(this);
//
//        reporter = AndroidEventReporter.getReporter(getApplicationContext());
//        reporter.report(EventType.APPLICATION, EventSeverity.INFO,
//                getApplicationContext().getString(R.string.app_started));
//
//        migrateToNewStyleRestUris();
//        ensureSavedUrisAreValid();
//        ensureIUnderstandDialogDisplayed();
//
//        mTracker = ((Nightscout) getApplicationContext()).getTracker();
//
//        // Register USB attached/detached and battery changes intents
//        IntentFilter deviceStatusFilter = new IntentFilter();
//        deviceStatusFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
//        deviceStatusFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
//        deviceStatusFilter.addAction(ACTION_POLL);
//        registerReceiver(mDeviceStatusReceiver, deviceStatusFilter);
//
//        // Register Broadcast Receiver for response messages from mSyncingServiceIntent service
////        mCGMStatusReceiver = new CGMStatusReceiver();
////        IntentFilter filter = new IntentFilter(CGMStatusReceiver.PROCESS_RESPONSE);
////        filter.addCategory(Intent.CATEGORY_DEFAULT);
////        registerReceiver(mCGMStatusReceiver, filter);
//
//        toastReceiver = new ToastReceiver();
//        IntentFilter toastFilter = new IntentFilter(ToastReceiver.ACTION_SEND_NOTIFICATION);
//        toastFilter.addCategory(Intent.CATEGORY_DEFAULT);
//        registerReceiver(toastReceiver, toastFilter);
//
////        mTextSGV.setTag(R.string.display_sgv, -1);
////        mTextSGV.setTag(R.string.display_trend, 0);
////        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
////        WebSettings webSettings = mWebView.getSettings();
////        webSettings.setJavaScriptEnabled(true);
////        webSettings.setDatabaseEnabled(true);
////        webSettings.setDomStorageEnabled(true);
////        webSettings.setAppCacheEnabled(true);
////        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
////        webSettings.setUseWideViewPort(false);
////        mWebView.setVerticalScrollBarEnabled(false);
////        mWebView.setHorizontalScrollBarEnabled(false);
////        mWebView.setBackgroundColor(0);
////        mWebView.loadUrl("file:///android_asset/index.html");
////        // disable scroll on touch
////        mWebView.setOnTouchListener(new View.OnTouchListener() {
////            @Override
////            public boolean onTouch(View v, MotionEvent event) {
////                return (event.getAction() == MotionEvent.ACTION_MOVE);
////            }
////        });
////        mUsbButton.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View v) {
////                Intent intent = new Intent(getApplicationContext(), UserEventPanelActivity.class);
////                intent.putExtra("Filter", EventType.DEVICE.ordinal());
////                startActivity(intent);
////            }
////        });
////        mSyncButton.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View v) {
////                Intent intent = new Intent(getApplicationContext(), UserEventPanelActivity.class);
////                intent.putExtra("Filter", EventType.UPLOADER.ordinal());
////                startActivity(intent);
////            }
////        });
//
//        // If app started due to android.hardware.usb.action.USB_DEVICE_ATTACHED intent, start syncing
//        Intent startIntent = getIntent();
//        String action = startIntent.getAction();
//        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) ||
//                SyncingService.isG4Connected(getApplicationContext())) {
//            reporter.report(EventType.DEVICE, EventSeverity.INFO,
//                    getApplicationContext().getString(R.string.g4_connected));
////            mUsbButton.setBackgroundResource(R.drawable.ic_usb);
//            Log.d(TAG, "Starting syncing in OnCreate...");
//            SyncingService.startActionSingleSync(getApplicationContext(), SyncingService.MIN_SYNC_PAGES);
//        }
//
//        // Check (only once) to see if they have opted in to shared data for research
//        if (!preferences.hasAskedForData()) {
//            // Prompt user to ask to donate data to research
//            AlertDialog.Builder dataDialog = new AlertDialog.Builder(this)
//                    .setCancelable(false)
//                    .setTitle(R.string.donate_dialog_title)
//                    .setMessage(R.string.donate_dialog_summary)
//                    .setPositiveButton(R.string.donate_dialog_yes, new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            mTracker.send(new HitBuilders.EventBuilder("DataDonateQuery", "Yes").build());
//                            preferences.setDataDonateEnabled(true);
//                        }
//                    })
//                    .setNegativeButton(R.string.donate_dialog_no, new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            mTracker.send(new HitBuilders.EventBuilder("DataDonateQuery", "No").build());
//                            preferences.setDataDonateEnabled(true);
//                        }
//                    })
//                    .setIcon(R.drawable.ic_launcher);
//
//            dataDialog.show();
//
//            preferences.setAskedForData(true);
//        }
//
//        // Report API vs mongo stats once per session
//        reportUploadMethods(mTracker);
//        pebble = new Pebble(getApplicationContext());
//        pebble.setUnits(preferences.getPreferredUnits());
//        pebble.setPwdName(preferences.getPwdName());
//
//        uploaderDevice = AndroidUploaderDevice.getUploaderDevice(getApplicationContext());
//
//        try {
//            setupMqtt();
////            mSyncButton.setBackgroundResource(R.drawable.ic_cloud);
//        } catch (MqttException e) {
////            mSyncButton.setBackgroundResource(R.drawable.ic_nocloud);
//        }
//
//        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//        Intent syncIntent = new Intent(MainActivity.ACTION_POLL);
//        syncManager = PendingIntent.getBroadcast(getApplicationContext(), 1, syncIntent, 0);
//
//        SyncingService.startActionSingleSync(getApplicationContext(), SyncingService.MIN_SYNC_PAGES);
//
////        FragmentManager fragmentManager = getFragmentManager();
////        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
////        MonitorFragment fragment = new MonitorFragment();
////        fragmentTransaction.add(R.id.monitor_fragment, fragment);
////        fragmentTransaction.commit();
//        startActivity(new Intent(this, NightscoutNavigationDrawer.class));
//    }
//
//    public void setupMqtt() throws MqttException {
//        if (preferences.isMqttEnabled()) {
//            if (!preferences.getMqttUser().equals("") && !preferences.getMqttPass().equals("") &&
//                    !preferences.getMqttEndpoint().equals("")) {
//                MqttConnectOptions mqttOptions = new MqttConnectOptions();
//                mqttOptions.setCleanSession(false);
//                mqttOptions.setKeepAliveInterval(150000);
//                mqttOptions.setUserName(preferences.getMqttUser());
//                mqttOptions.setPassword(preferences.getMqttPass().toCharArray());
//                String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
//                MemoryPersistence dataStore = new MemoryPersistence();
//                MqttClient client = new MqttClient(preferences.getMqttEndpoint(), androidId, dataStore);
//                MqttPinger pinger = new AndroidMqttPinger(getApplicationContext(), 0, client, 150000);
//                MqttTimer timer = new AndroidMqttTimer(getApplicationContext(), 0);
//                mqttManager = new MqttEventMgr(client, mqttOptions, pinger, timer, reporter);
//                mqttManager.connect();
////                mSyncButton.setBackgroundResource(R.drawable.ic_cloud);
//            }
//        }
//    }
//
//    public void reportUploadMethods(Tracker tracker) {
//        if (preferences.isRestApiEnabled()) {
//            for (String url : preferences.getRestApiBaseUris()) {
//                String apiVersion = (RestUriUtils.isV1Uri(URI.create(url))) ? "WebAPIv1" : "Legacy WebAPI";
//                tracker.send(new HitBuilders.EventBuilder("Upload", apiVersion).build());
//            }
//        }
//        if (preferences.isMongoUploadEnabled()) {
//            tracker.send(new HitBuilders.EventBuilder("Upload", "Mongo").build());
//        }
//    }
//
//    private void migrateToNewStyleRestUris() {
//        List<String> newUris = new ArrayList<>();
//        for (String uriString : preferences.getRestApiBaseUris()) {
//            if (uriString.contains("@http")) {
//                List<String> splitUri = Splitter.on('@').splitToList(uriString);
//                Uri oldUri = Uri.parse(splitUri.get(1));
//                String newAuthority = Joiner.on('@').join(splitUri.get(0), oldUri.getEncodedAuthority());
//                Uri newUri = oldUri.buildUpon().encodedAuthority(newAuthority).build();
//                newUris.add(newUri.toString());
//            } else {
//                newUris.add(uriString);
//            }
//        }
//        preferences.setRestApiBaseUris(newUris);
//    }
//
//    private void ensureSavedUrisAreValid() {
//        if (PreferencesValidator.validateMongoUriSyntax(getApplicationContext(),
//                preferences.getMongoClientUri()).isPresent()) {
//            preferences.setMongoClientUri(null);
//        }
//        List<String> filteredRestUris = new ArrayList<>();
//        for (String uri : preferences.getRestApiBaseUris()) {
//            if (!PreferencesValidator.validateRestApiUriSyntax(getApplicationContext(), uri).isPresent()) {
//                filteredRestUris.add(uri);
//            }
//        }
//        preferences.setRestApiBaseUris(filteredRestUris);
//        if (PreferencesValidator.validateMqttEndpointSyntax(getApplicationContext(),
//                preferences.getMqttEndpoint()).isPresent()) {
//            preferences.setMqttEndpoint(null);
//        }
//    }
//
//    private void ensureIUnderstandDialogDisplayed() {
//        if (!preferences.getIUnderstand()) {
//            // Prompt user to ask to donate data to research
//            AlertDialog.Builder dataDialog = new AlertDialog.Builder(this)
//                    .setCancelable(false)
//                    .setTitle(R.string.pref_title_i_understand)
//                    .setMessage(R.string.pref_summary_i_understand)
//                    .setPositiveButton(R.string.donate_dialog_yes, new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            preferences.setIUnderstand(true);
//                        }
//                    })
//                    .setNegativeButton(R.string.donate_dialog_no, new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            android.os.Process.killProcess(android.os.Process.myPid());
//                            System.exit(1);
//                        }
//                    })
//                    .setIcon(R.drawable.ic_launcher);
//            dataDialog.show();
//        }
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        Log.d(TAG, "onPaused called.");
////        mWebView.pauseTimers();
////        mWebView.onPause();
////        mHandler.removeCallbacks(updateTimeAgo);
//    }
//
//    public void setPebble(Pebble pebble) {
//        this.pebble = pebble;
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        Log.d(TAG, "onResumed called.");
////        mWebView.onResume();
////        mWebView.resumeTimers();
//
//        // Set and deal with mmol/L<->mg/dL conversions
//        Log.d(TAG, "display_options_units: " + preferences.getPreferredUnits().name());
//        pebble.config(preferences.getPwdName(), preferences.getPreferredUnits(), getApplicationContext());
////        int sgv = (Integer) mTextSGV.getTag(R.string.display_sgv);
//
////        int direction = (Integer) mTextSGV.getTag(R.string.display_trend);
////        if (sgv != -1) {
////            GlucoseReading sgvReading = new GlucoseReading(sgv, GlucoseUnit.MGDL);
////            mTextSGV.setText(getSGVStringByUnit(sgvReading, TrendArrow.values()[direction]));
////        }
//
////        mWebView.loadUrl("javascript:updateUnits(" + Boolean.toString(preferences.getPreferredUnits() == GlucoseUnit.MMOL) + ")");
//
////        mHandler.post(updateTimeAgo);
//        try {
//            if (mqttManager != null && mqttManager.isConnected()) {
//                MqttClient client = mqttManager.getClient();
//                MqttConnectOptions options = mqttManager.getOptions();
//                boolean mqttOptsChanged = !preferences.getMqttEndpoint().equals(client.getServerURI());
//                mqttOptsChanged &= !preferences.getMqttUser().equals(options.getUserName());
//                mqttOptsChanged &= !preferences.getMqttPass().equals(String.valueOf(options.getPassword()));
//                if (mqttOptsChanged) {
//                    mqttManager.disconnect();
//                    setupMqtt();
//                }
//            }
//
//            if ((mqttManager == null || !mqttManager.isConnected()) && preferences.isMqttEnabled()) {
//                setupMqtt();
//            }
//
//            if (mqttManager != null && mqttManager.isConnected() && !preferences.isMqttEnabled()) {
//                mqttManager.close();
//            }
//        } catch (MqttException e) {
//            reporter.report(EventType.UPLOADER, EventSeverity.WARN,
//                    getApplicationContext().getString(R.string.unhandled_mqtt_exception,
//                            e.getMessage()));
////            mSyncButton.setBackgroundResource(R.drawable.ic_nocloud);
//        }
//
//    }
//
////    private String getSGVStringByUnit(GlucoseReading sgv, TrendArrow trend) {
////        String sgvStr = sgv.asStr(preferences.getPreferredUnits());
////        return (sgv.asMgdl() != -1) ?
////                (isSpecialValue(sgv)) ?
////                        getEGVSpecialValue(sgv).get().toString() : sgvStr + " " + trend.symbol() : "---";
////    }
//
//    @Override
//    protected void onDestroy() {
//        Log.d(TAG, "onDestroy called.");
//        super.onDestroy();
////        unregisterReceiver(mCGMStatusReceiver);
//        unregisterReceiver(mDeviceStatusReceiver);
//        unregisterReceiver(toastReceiver);
//        if (pebble != null) {
//            pebble.close();
//        }
//        if (mqttManager != null) {
//            mqttManager.close();
//        }
//        if (uploaderDevice != null) {
//            uploaderDevice.close();
//        }
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        GoogleAnalytics.getInstance(this).reportActivityStop(this);
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        GoogleAnalytics.getInstance(this).reportActivityStart(this);
//    }
//
//    @Override
//    public void onSaveInstanceState(@NonNull Bundle outState) {
//        super.onSaveInstanceState(outState);
////        mWebView.saveState(outState);
////        outState.putString("saveJSONData", mJSONData);
////        outState.putString("saveTextSGV", mTextSGV.getText().toString());
////        outState.putString("saveTextTimestamp", mTextTimestamp.getText().toString());
//    }
//
//    @Override
//    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//        // Restore the state of the WebView
////        mWebView.restoreState(savedInstanceState);
////        mJSONData = savedInstanceState.getString("mJSONData");
////        mTextSGV.setText(savedInstanceState.getString("saveTextSGV"));
////        mTextTimestamp.setText(savedInstanceState.getString("saveTextTimestamp"));
//    }
//
//    BroadcastReceiver mDeviceStatusReceiver = new BroadcastReceiver() {
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//
//            switch (action) {
//                case UsbManager.ACTION_USB_DEVICE_DETACHED:
//                    reporter.report(EventType.DEVICE, EventSeverity.INFO,
//                            getApplicationContext().getString(R.string.g4_disconnected));
//                    cancelPoll();
////                    mUsbButton.setBackgroundResource(R.drawable.ic_nousb);
//                    break;
//                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
//                    reporter.report(EventType.DEVICE, EventSeverity.INFO,
//                            getApplicationContext().getString(R.string.g4_connected));
////                    mUsbButton.setBackgroundResource(R.drawable.ic_usb);
//                    Log.d(TAG, "Starting syncing on USB attached...");
//                    SyncingService.startActionSingleSync(getApplicationContext(), SyncingService.MIN_SYNC_PAGES);
//                    break;
//                case MainActivity.ACTION_POLL:
//                    SyncingService.startActionSingleSync(getApplicationContext(), SyncingService.MIN_SYNC_PAGES);
//            }
//        }
//    };
//
//    public Runnable updateTimeAgo = new Runnable() {
//        @Override
//        public void run() {
//            // FIXME: doesn't calculate timeago properly.
//            long delta = new Date().getTime() - lastRecordTime;
//            if (lastRecordTime == 0) delta = 0;
//
//            String timeAgoStr;
//            if (lastRecordTime == -1) {
//                timeAgoStr = "---";
//            } else if (delta < 0) {
//                timeAgoStr = getString(R.string.TimeChangeDetected);
//            } else {
//                timeAgoStr = Utils.getTimeString(delta);
//            }
////            mTextTimestamp.setText(timeAgoStr);
//            mHandler.removeCallbacks(updateTimeAgo);
//            long delay = delta % standardMinutes(1).getMillis();
//            mHandler.postDelayed(updateTimeAgo, delay);
//        }
//    };
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        Intent intent;
//        switch (item.getItemId()) {
//            case R.id.menu_settings:
//                intent = new Intent(this, SettingsActivity.class);
//                startActivity(intent);
//                break;
//            case R.id.feedback_settings:
//                feedbackDialog.show();
//                break;
//            case R.id.gap_sync:
//                SyncingService.startActionSingleSync(getApplicationContext(),
//                        SyncingService.GAP_SYNC_PAGES);
//                break;
//            case R.id.event_log:
//                intent = new Intent(getApplicationContext(), UserEventPanelActivity.class);
//                intent.putExtra("Filter", EventType.ALL.ordinal());
//                startActivity(intent);
//                break;
//            case R.id.usb_log:
//                intent = new Intent(getApplicationContext(), UserEventPanelActivity.class);
//                intent.putExtra("Filter", EventType.DEVICE.ordinal());
//                startActivity(intent);
//                break;
//            case R.id.upload_log:
//                intent = new Intent(getApplicationContext(), UserEventPanelActivity.class);
//                intent.putExtra("Filter", EventType.UPLOADER.ordinal());
//                startActivity(intent);
//                break;
//            case R.id.close_settings:
//                cancelPoll();
//                finish();
//                break;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    @TargetApi(Build.VERSION_CODES.KITKAT)
//    public void setNextPoll(long millis) {
//        Log.d(TAG, "Setting next poll with Alarm for " + millis + " ms from now.");
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millis, syncManager);
//        } else {
//            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millis, syncManager);
//        }
//    }
//
//    @Subscribe
//    public void incomingData(final G4Download download) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                boolean published = false;
//                long refTime = DateTime.parse(download.download_timestamp).getMillis();
//                if (preferences.isMqttEnabled() && download.toByteArray() != null && download.toByteArray().length != 0) {
//                    Log.d(TAG, "Proto: " + Utils.bytesToHex(download.toByteArray()));
//                    if (mqttManager != null) {
//                        Log.d(TAG, "Publishing");
//                        mqttManager.publish(download.toByteArray(), "/downloads/protobuf");
//                        ((AndroidPreferences) preferences).setLastEgvMqttUpload(download.sgv.get(download.sgv.size() - 1).sys_timestamp_sec);
//                        ((AndroidPreferences) preferences).setLastMeterMqttUpload(download.meter.get(download.meter.size() - 1).sys_timestamp_sec);
//                        ((AndroidPreferences) preferences).setLastSensorMqttUpload(download.sensor.get(download.sensor.size() - 1).sys_timestamp_sec);
//                        ((AndroidPreferences) preferences).setLastCalMqttUpload(download.cal.get(download.cal.size() - 1).sys_timestamp_sec);
//                        published = true;
//                    } else {
//                        reporter.report(EventType.DEVICE, EventSeverity.ERROR,
//                                getApplicationContext().getString(R.string.mqtt_mgr_not_initialized));
//                    }
//                }
//                lastRecordTime = new DateTime().getMillis() - Duration.standardSeconds(download.receiver_system_time_sec - download.sgv.get(download.sgv.size() - 1).sys_timestamp_sec).getMillis();
//                EGVRecord recentRecord = null;
//                if (download.sgv.size() > 0) {
//                    recentRecord = new EGVRecord(download.sgv.get(download.sgv.size() - 1), download.receiver_system_time_sec, refTime);
//                    pebble.sendDownload(recentRecord.getReading(), recentRecord.getTrend(), recentRecord.getWallTime().getMillis(), getApplicationContext());
//                    // Reload d3 chart with new data
//                    JSONArray array = new JSONArray();
//                    for (SensorGlucoseValueEntry sgve : download.sgv) {
//                        try {
//                            EGVRecord record = new EGVRecord(sgve, sgve.sys_timestamp_sec, refTime);
//                            array.put(record.toJSON());
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                    }
////                    mWebView.loadUrl("javascript:updateData(" + array + ")");
////                    mTextSGV.setText(getSGVStringByUnit(recentRecord.getReading(), recentRecord.getTrend()));
////                    mTextSGV.setTag(R.string.display_sgv, recentRecord.getReading().asMgdl());
////                    mTextSGV.setTag(R.string.display_trend, recentRecord.getTrend().ordinal());
//                }
//
//                if (download.sgv.get(download.sgv.size() - 1).sgv_mgdl != -1) {
//                    GlucoseReading reading = new GlucoseReading(download.sgv.get(download.sgv.size() - 1).sgv_mgdl, GlucoseUnit.MGDL);
//                    pebble.sendDownload(reading, TrendArrow.values()[download.sgv.get(download.sgv.size() - 1).trend.ordinal()], Utils.systemTimeToWallTime(download.sgv.get(download.sgv.size() - 1).sys_timestamp_sec, download.receiver_system_time_sec, refTime).getMillis(), getApplicationContext());
//                }
//
//                // Update UI with latest record information
//
////        if (preferences.isMqttEnabled()) {
////            responseUploadStatus &= published;
////        }
////        if (responseUploadStatus) {
////            mSyncButton.setBackgroundResource(R.drawable.ic_cloud);
////        } else {
////            mSyncButton.setBackgroundResource(R.drawable.ic_nocloud);
////        }
//                EGVRecord lastEgvRecord = new EGVRecord(download.sgv.get(download.sgv.size() - 1), download.receiver_system_time_sec, refTime);
////                String timeAgoStr = "---";
////                if (lastEgvRecord.getRawSystemTimeSeconds() > 0) {
////                    timeAgoStr = Utils.getTimeString(download.receiver_system_time_sec - lastEgvRecord.getRawSystemTimeSeconds());
////                }
////
////                mTextTimestamp.setText(timeAgoStr);
////                mTextTimestamp.setTag(timeAgoStr);
//
//                long nextUploadTime = Duration.standardSeconds(Minutes.minutes(5).toStandardSeconds().getSeconds() - ((download.receiver_system_time_sec - lastEgvRecord.getRawSystemTimeSeconds()) % Minutes.minutes(5).toStandardSeconds().getSeconds())).getMillis();
//                setNextPoll(nextUploadTime);
//            }
//        });
//
//    }
//
//
//    public void cancelPoll() {
//        Log.d(TAG, "Canceling next alarm poll.");
//        alarmManager.cancel(syncManager);
//    }
}
