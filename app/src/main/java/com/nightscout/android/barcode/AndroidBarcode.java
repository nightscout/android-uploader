package com.nightscout.android.barcode;

import android.app.Activity;

import com.google.zxing.integration.android.IntentIntegrator;

public class AndroidBarcode {
    static public final String SCAN_INTENT="com.google.zxing.client.android.SCAN";
    Activity activity;

    public AndroidBarcode(Activity activity){
        this.activity = activity;
    }

    public void scan(){
        new IntentIntegrator(activity).initiateScan();
    }
}