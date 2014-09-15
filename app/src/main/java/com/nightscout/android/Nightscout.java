package com.nightscout.android;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.*;

@ReportsCrashes(
        formKey = "",
        formUri = "http://nightscout.cloudant.com/acra-nightscout/_design/acra-storage/_update/report",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin="whisphisheiringliketfurg",
        formUriBasicAuthPassword="8CgjF6r2u4i8EhPHoPJjnk8f",
        resToastText = R.string.crash_toast_text,
        resDialogText = R.string.feebback_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_alert,
        resDialogTitle = R.string.feedback_dialog_title,
        resDialogCommentPrompt = R.string.feedback_dialog_comment_prompt,
        resDialogOkToast = R.string.feedback_dialog_ok_toast,
        mode = ReportingInteractionMode.TOAST
)

public class Nightscout extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
    }
}
