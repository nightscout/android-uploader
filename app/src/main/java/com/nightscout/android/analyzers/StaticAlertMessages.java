package com.nightscout.android.analyzers;

import com.nightscout.android.processors.AlertMessage;

import java.util.ArrayList;

public class StaticAlertMessages {
    static protected ArrayList<AlertMessage> alertMessages=new ArrayList<AlertMessage>();

    synchronized static public void addMessage(AlertMessage msg){
        if (! alertMessages.contains(msg))
            alertMessages.add(msg);
    }

    synchronized static public void clearMessages(){
        alertMessages=new ArrayList<AlertMessage>();
    }

    synchronized static public void removeMessage(AlertMessage msg){
        if (alertMessages.contains(msg))
            alertMessages.remove(msg);
    }

    static public ArrayList<AlertMessage> getAlertMessages(){
        return alertMessages;
    }
}