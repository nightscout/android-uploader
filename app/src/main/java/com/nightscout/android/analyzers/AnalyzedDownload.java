package com.nightscout.android.analyzers;

import android.util.Log;

import com.nightscout.android.dexcom.G4Download;
import com.nightscout.android.processors.AlertLevel;
import com.nightscout.android.processors.AlertMessage;

import java.util.ArrayList;
import java.util.Iterator;

public class AnalyzedDownload extends G4Download {
    protected static final String TAG = AnalyzedDownload.class.getSimpleName();
    protected ArrayList<AlertMessage> messages=new ArrayList<AlertMessage>();
    protected ArrayList<Condition> conditions=new ArrayList<Condition>();

    AnalyzedDownload(G4Download dl){
        super(dl);
    }

    public ArrayList<AlertMessage> getMessages() {
        return messages;
    }

    public void addMessage(AlertMessage message){
        if (! conditions.contains(message.getCondition()) && message.getCondition()!= Condition.NONE)
            conditions.add(message.getCondition());
        if (!messages.contains(message))
            this.messages.add(message);
    }

    public void setMessages(ArrayList<AlertMessage> messages) {
        this.messages = messages;
    }

    public ArrayList<AlertMessage> getAlertsForLevel(AlertLevel alertLevel){
        ArrayList<AlertMessage> response=new ArrayList<AlertMessage>();
        for (AlertMessage message:messages){
            if (message.getAlertLevel()==alertLevel)
                response.add(message);
        }
        return response;
    }

    public ArrayList<Condition> getConditions() {
        return conditions;
    }

    public ArrayList<AlertMessage> getMessagesByCondition(Condition... conditions){
        ArrayList<AlertMessage> result=new ArrayList<AlertMessage>();
        for (AlertMessage msg:messages){
            for (Condition condition:conditions) {
                if (msg.getCondition() == condition)
                    result.add(msg);
            }
        }
        return result;
    }

    public ArrayList<AlertMessage> getMessagesByCriticality(AlertLevel... levels){
        ArrayList<AlertMessage> result=new ArrayList<AlertMessage>();
        for (AlertMessage msg:messages){
            for (AlertLevel level:levels) {
                if (msg.getAlertLevel() == level)
                    result.add(msg);
            }
        }
        return result;
    }

    public int removeMessageByCondition(Condition... conditions){
        int count=0;

        for (Iterator<AlertMessage> iterator = messages.iterator(); iterator.hasNext(); ) {
            for (Condition condition:conditions) {
                AlertMessage record = iterator.next();
                if (record.condition == condition) {
                    Log.d(TAG, "Removed message: "+record.getMessage());
                    Log.d(TAG, "Search condition: "+condition+" Message condition: "+record.condition);
                    iterator.remove();
                }
            }
        }
        return count;
    }

    public void addCondition(Condition condition) {
        this.conditions.add(condition);
    }
}