package com.nightscout.android.upload;

public class PushoverResponse {

    private int status;
    private String[] errors;

    public int getStatus() {
        return status;
    }

    public String[] getErrors() {
        return errors;
    }

    public PushoverResponse(int status, String[] errors) {
        this.status = status;
        this.errors = errors;
    }
}
