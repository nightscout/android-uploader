package com.nightscout.android.barcode;

import android.app.Activity;

import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;

public class BarcodeScanner {
    public static final String SCAN_INTENT = Intents.Scan.ACTION;
    Activity activity;

    public BarcodeScanner(Activity activity){
        this.activity = activity;
    }

    public void scan() {
        new IntentIntegrator(activity).initiateScan();
    }
}