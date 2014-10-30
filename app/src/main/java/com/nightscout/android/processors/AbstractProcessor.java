package com.nightscout.android.processors;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

abstract public class AbstractProcessor implements ProcessorInterface {
    protected String name;
    protected Context context;
    protected SharedPreferences sharedPref;

    public AbstractProcessor(Context context,String name){
        this.name=name;
        this.context=context;
        PreferenceManager.getDefaultSharedPreferences(context);
    }
}
