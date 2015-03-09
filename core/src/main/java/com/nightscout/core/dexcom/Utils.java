package com.nightscout.core.dexcom;

import com.google.common.base.Strings;
import com.google.common.hash.HashCode;
import com.nightscout.core.dexcom.records.GlucoseDataSet;
import com.nightscout.core.model.SensorEntry;
import com.nightscout.core.model.SensorGlucoseValueEntry;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.joda.time.Duration.standardSeconds;

public final class Utils {
    protected static final Logger log = LoggerFactory.getLogger(Utils.class);

    public static final DateTime DEXCOM_EPOCH = new DateTime(2009, 1, 1, 0, 0, 0, 0).withZone(DateTimeZone.UTC);

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

    // TODO: probably not the right way to do this but it seems to do the trick. Need to revisit this to fully understand what is going on during DST change
    public static DateTime receiverTimeToDateTime(long deltaInSeconds) {
        int offset = DateTimeZone.getDefault().getOffset(DEXCOM_EPOCH) - DateTimeZone.getDefault().getOffset(Instant.now());
        return DEXCOM_EPOCH.plus(offset).plus(standardSeconds(deltaInSeconds)).withZone(DateTimeZone.UTC);
    }

    public static Date receiverTimeToDate(long delta) {
        return receiverTimeToDateTime(delta).toDate();
    }

    /**
     * Returns human-friendly string for the length of this duration, e.g. 4 seconds ago
     * or 4 days ago.
     *
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
        minutes = minutes - hours * 60;
        hours = hours - days * 24;
        days = days - weeks * 7;

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

    public static List<GlucoseDataSet> mergeGlucoseDataRecords(List<SensorGlucoseValueEntry> egvRecords,
                                                               List<SensorEntry> sensorRecords) {
        int egvLength = egvRecords.size();
        int sensorLength = sensorRecords.size();
        List<GlucoseDataSet> glucoseDataSets = new ArrayList<>();
        if (egvLength >= 0 && sensorLength == 0) {
            for (int i = 1; i <= egvLength; i++) {
                glucoseDataSets.add(new GlucoseDataSet(egvRecords.get(egvLength - i)));
            }
            return glucoseDataSets;
        }
        int smallerLength = egvLength < sensorLength ? egvLength : sensorLength;
        for (int i = 1; i <= smallerLength; i++) {
            glucoseDataSets.add(new GlucoseDataSet(egvRecords.get(egvLength - i),
                    sensorRecords.get(sensorLength - i)));
        }
        return glucoseDataSets;
    }


    public static String bytesToHex(byte[] bytes) {
        return HashCode.fromBytes(bytes).toString().toUpperCase();
    }
}
