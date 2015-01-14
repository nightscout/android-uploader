package com.nightscout.core.dexcom;

import com.google.common.base.Strings;
import com.google.common.hash.HashCode;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.dexcom.records.SensorRecord;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.joda.time.Duration.standardSeconds;

public class Utils {
    public static final DateTime DEXCOM_EPOCH = new DateTime(2009, 1, 1, 0, 0, 0, 0);

    private static final String PRIMARY_SEPARATOR = ", ";
    private static final String SECONDARY_SEPARATOR = ", and ";
    private static final PeriodFormatter FORMATTER = new PeriodFormatterBuilder()
        .appendSeconds().appendSuffix(" seconds").appendSeparator(PRIMARY_SEPARATOR, SECONDARY_SEPARATOR)
        .appendMinutes().appendSuffix(" minutes").appendSeparator(PRIMARY_SEPARATOR, SECONDARY_SEPARATOR)
        .appendHours().appendSuffix(" hours").appendSeparator(PRIMARY_SEPARATOR, SECONDARY_SEPARATOR)
        .appendDays().appendSuffix(" days").appendSeparator(PRIMARY_SEPARATOR, SECONDARY_SEPARATOR)
        .appendWeeks().appendSuffix(" weeks").appendSeparator(PRIMARY_SEPARATOR, SECONDARY_SEPARATOR)
        .appendMonths().appendSuffix(" months").appendSeparator(PRIMARY_SEPARATOR, SECONDARY_SEPARATOR)
        .appendYears().appendSuffix(" years").appendLiteral(" ago")
        .printZeroNever()
        .toFormatter();

    public static DateTime receiverTimeToDateTime(long deltaInSeconds) {
        return DEXCOM_EPOCH.plus(standardSeconds(deltaInSeconds)).withZone(DateTimeZone.getDefault());
    }

    public static Date receiverTimeToDate(long delta) {
        return receiverTimeToDateTime(delta).toDate();
    }

    /**
     * Returns human-friendly string for the length of this duration, e.g. 4 seconds ago
     * or 4 days ago.
     * @param period Non-null Period instance.
     * @return String human-friendly Period string, e.g. 4 seconds ago.
     */
    public static String getTimeAgoString(Period period) {
        checkNotNull(period);
        String output = FORMATTER.print(period);
        if (Strings.isNullOrEmpty(output)) {
            return "--";
        }
        return output;
    }

    public static String getTimeString(long timeDeltaMS) {
        long minutes = (timeDeltaMS / 1000) / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        minutes= minutes - hours * 60;
        hours = hours - days * 24;
        days= days - weeks * 7;

        String timeAgoString = "";
        if (weeks > 0) {
            timeAgoString += weeks + " weeks ";
        }
        if (days > 0) {
            timeAgoString += days + " days ";
        }
        if (hours > 0) {
            timeAgoString += hours + " hours ";
        }
        if (minutes >= 0) {
            timeAgoString += minutes + " min ";
        }

        return (timeAgoString.equals("") ? "--" : timeAgoString + "ago");
    }

    public static GlucoseDataSet[] mergeGlucoseDataRecords(EGVRecord[] egvRecords,
                                                           SensorRecord[] sensorRecords) {
        int egvLength = egvRecords.length;
        int sensorLength = sensorRecords.length;
        int smallerLength = egvLength < sensorLength ? egvLength : sensorLength;
        GlucoseDataSet[] glucoseDataSets = new GlucoseDataSet[smallerLength];
        for (int i = 1; i <= smallerLength; i++) {
            glucoseDataSets[smallerLength - i] = new GlucoseDataSet(egvRecords[egvLength - i], sensorRecords[sensorLength - i]);
        }
        return glucoseDataSets;
    }

    public static String bytesToHex(byte[] bytes) {
        return HashCode.fromBytes(bytes).toString().toUpperCase();
    }
}
