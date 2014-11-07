package com.nightscout.android.processors;

import com.nightscout.android.analyzers.Condition;

public class AlertMessage {
    public AlertLevel alertLevel;
    public String message;
    public Condition condition;

    public AlertMessage(AlertLevel aL,String msg,Condition condition){
        this.alertLevel=aL;
        this.message=msg;
        this.condition=condition;
    }

    public AlertLevel getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(AlertLevel alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlertMessage that = (AlertMessage) o;

        if (alertLevel != that.alertLevel) return false;
        if (condition != that.condition) return false;
        if (!message.equals(that.message)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = alertLevel.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + condition.hashCode();
        return result;
    }
}