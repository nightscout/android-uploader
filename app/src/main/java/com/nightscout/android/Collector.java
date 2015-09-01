package com.nightscout.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import org.joda.time.Minutes;

public class Collector extends WakefulBroadcastReceiver {

  private static final long POLL_TIME = Minutes.minutes(5).toStandardDuration().getMillis();

  @Override
  public void onReceive(Context context, Intent intent) {
    Intent serviceIntent = new Intent(context, CollectorService.class);
    startWakefulService(context, serviceIntent);
  }

  public void setAlarm(Context context) {
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), POLL_TIME,
                     getPendingIntent(context));
  }

  public void cancelAlarm(Context context) {
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    alarmManager.cancel(getPendingIntent(context));
  }

  private PendingIntent getPendingIntent(Context context) {
    Intent intent = new Intent(context, Collector.class);
    return PendingIntent.getBroadcast(context, 0, intent, 0);
  }
}
