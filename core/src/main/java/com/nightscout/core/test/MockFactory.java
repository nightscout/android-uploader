package com.nightscout.core.test;

import com.nightscout.core.dexcom.NoiseMode;
import com.nightscout.core.dexcom.TrendArrow;
import com.nightscout.core.dexcom.records.*;
import com.nightscout.core.records.DeviceStatus;
import org.joda.time.DateTime;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MockFactory {
    public static GlucoseDataSet mockGlucoseDataSet() {
        EGVRecord egvRecord = new EGVRecord(
                1,
                TrendArrow.DOUBLE_DOWN,
                new DateTime(0).toDate(),
                new DateTime(5).toDate(),
                NoiseMode.CLEAN);
        SensorRecord sensorRecord = new SensorRecord(new byte[SensorRecord.RECORD_SIZE]);
        return new GlucoseDataSet(egvRecord, sensorRecord);
    }

    public static MeterRecord mockMeterRecord() {
        return new MeterRecord(new byte[MeterRecord.RECORD_SIZE]);
    }

    public static CalRecord mockCalRecord() {
        return new CalRecord(new byte[CalRecord.RECORD_SIZE]);
    }

    public static DeviceStatus mockDeviceStatus() {
        DeviceStatus output = new DeviceStatus();
        output.setBatteryLevel(999);
        return output;
    }

    public static EGVRecord[] mockEGVPage() throws ParseException {
        DateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        EGVRecord[] record = new EGVRecord[61];
        record[0] = new EGVRecord(108, TrendArrow.DOWN_45, format.parse("Thu Dec 04 11:19:53 CST 2014"), format.parse("Thu Dec 04 17:19:55 CST 2014"), NoiseMode.CLEAN);
        record[1] = new EGVRecord(115, TrendArrow.FLAT, format.parse("Thu Dec 04 11:24:53 CST 2014"), format.parse("Thu Dec 04 17:24:55 CST 2014"), NoiseMode.CLEAN);
        record[2] = new EGVRecord(137, TrendArrow.UP_45, format.parse("Thu Dec 04 11:34:53 CST 2014"), format.parse("Thu Dec 04 17:34:55 CST 2014"), NoiseMode.CLEAN);
        record[3] = new EGVRecord(147, TrendArrow.UP_45, format.parse("Thu Dec 04 11:39:53 CST 2014"), format.parse("Thu Dec 04 17:39:55 CST 2014"), NoiseMode.CLEAN);
        record[4] = new EGVRecord(245, TrendArrow.NOT_COMPUTABLE, format.parse("Thu Dec 04 16:14:52 CST 2014"), format.parse("Thu Dec 04 22:14:54 CST 2014"), NoiseMode.CLEAN);
        record[5] = new EGVRecord(247, TrendArrow.NOT_COMPUTABLE, format.parse("Thu Dec 04 16:19:52 CST 2014"), format.parse("Thu Dec 04 22:19:55 CST 2014"), NoiseMode.CLEAN);
        record[6] = new EGVRecord(244, TrendArrow.NOT_COMPUTABLE, format.parse("Thu Dec 04 16:24:52 CST 2014"), format.parse("Thu Dec 04 22:24:54 CST 2014"), NoiseMode.CLEAN);
        record[7] = new EGVRecord(243, TrendArrow.NOT_COMPUTABLE, format.parse("Thu Dec 04 16:29:52 CST 2014"), format.parse("Thu Dec 04 22:29:54 CST 2014"), NoiseMode.CLEAN);
        record[8] = new EGVRecord(240, TrendArrow.FLAT, format.parse("Thu Dec 04 16:34:52 CST 2014"), format.parse("Thu Dec 04 22:34:54 CST 2014"), NoiseMode.CLEAN);
        record[9] = new EGVRecord(229, TrendArrow.DOWN_45, format.parse("Thu Dec 04 16:39:52 CST 2014"), format.parse("Thu Dec 04 22:39:54 CST 2014"), NoiseMode.CLEAN);
        record[10] = new EGVRecord(213, TrendArrow.SINGLE_DOWN, format.parse("Thu Dec 04 16:44:52 CST 2014"), format.parse("Thu Dec 04 22:44:54 CST 2014"), NoiseMode.CLEAN);
        record[11] = new EGVRecord(202, TrendArrow.SINGLE_DOWN, format.parse("Thu Dec 04 16:49:52 CST 2014"), format.parse("Thu Dec 04 22:49:54 CST 2014"), NoiseMode.CLEAN);
        record[12] = new EGVRecord(206, TrendArrow.DOWN_45, format.parse("Thu Dec 04 16:54:52 CST 2014"), format.parse("Thu Dec 04 22:54:54 CST 2014"), NoiseMode.CLEAN);
        record[13] = new EGVRecord(207, TrendArrow.FLAT, format.parse("Thu Dec 04 16:59:52 CST 2014"), format.parse("Thu Dec 04 22:59:54 CST 2014"), NoiseMode.CLEAN);
        record[14] = new EGVRecord(205, TrendArrow.FLAT, format.parse("Thu Dec 04 17:04:52 CST 2014"), format.parse("Thu Dec 04 23:04:54 CST 2014"), NoiseMode.CLEAN);
        record[15] = new EGVRecord(208, TrendArrow.FLAT, format.parse("Thu Dec 04 17:09:52 CST 2014"), format.parse("Thu Dec 04 23:09:54 CST 2014"), NoiseMode.CLEAN);
        record[16] = new EGVRecord(212, TrendArrow.FLAT, format.parse("Thu Dec 04 17:14:52 CST 2014"), format.parse("Thu Dec 04 23:14:54 CST 2014"), NoiseMode.CLEAN);
        record[17] = new EGVRecord(216, TrendArrow.FLAT, format.parse("Thu Dec 04 17:19:52 CST 2014"), format.parse("Thu Dec 04 23:19:54 CST 2014"), NoiseMode.CLEAN);
        record[18] = new EGVRecord(219, TrendArrow.FLAT, format.parse("Thu Dec 04 17:24:52 CST 2014"), format.parse("Thu Dec 04 23:24:54 CST 2014"), NoiseMode.CLEAN);
        record[19] = new EGVRecord(222, TrendArrow.FLAT, format.parse("Thu Dec 04 17:29:52 CST 2014"), format.parse("Thu Dec 04 23:29:54 CST 2014"), NoiseMode.CLEAN);
        record[20] = new EGVRecord(225, TrendArrow.FLAT, format.parse("Thu Dec 04 17:34:52 CST 2014"), format.parse("Thu Dec 04 23:34:54 CST 2014"), NoiseMode.CLEAN);
        record[21] = new EGVRecord(225, TrendArrow.FLAT, format.parse("Thu Dec 04 17:39:52 CST 2014"), format.parse("Thu Dec 04 23:39:54 CST 2014"), NoiseMode.CLEAN);
        record[22] = new EGVRecord(224, TrendArrow.FLAT, format.parse("Thu Dec 04 17:44:52 CST 2014"), format.parse("Thu Dec 04 23:44:54 CST 2014"), NoiseMode.CLEAN);
        record[23] = new EGVRecord(239, TrendArrow.FLAT, format.parse("Thu Dec 04 17:54:52 CST 2014"), format.parse("Thu Dec 04 23:54:54 CST 2014"), NoiseMode.CLEAN);
        record[24] = new EGVRecord(249, TrendArrow.UP_45, format.parse("Thu Dec 04 17:59:52 CST 2014"), format.parse("Thu Dec 04 23:59:54 CST 2014"), NoiseMode.CLEAN);
        record[25] = new EGVRecord(255, TrendArrow.UP_45, format.parse("Thu Dec 04 18:04:52 CST 2014"), format.parse("Fri Dec 05 00:04:54 CST 2014"), NoiseMode.CLEAN);
        record[26] = new EGVRecord(272, TrendArrow.UP_45, format.parse("Thu Dec 04 18:14:52 CST 2014"), format.parse("Fri Dec 05 00:14:54 CST 2014"), NoiseMode.CLEAN);
        record[27] = new EGVRecord(283, TrendArrow.SINGLE_UP, format.parse("Thu Dec 04 18:19:52 CST 2014"), format.parse("Fri Dec 05 00:19:54 CST 2014"), NoiseMode.CLEAN);
        record[28] = new EGVRecord(284, TrendArrow.UP_45, format.parse("Thu Dec 04 18:24:52 CST 2014"), format.parse("Fri Dec 05 00:24:54 CST 2014"), NoiseMode.CLEAN);
        record[29] = new EGVRecord(272, TrendArrow.FLAT, format.parse("Thu Dec 04 18:29:52 CST 2014"), format.parse("Fri Dec 05 00:29:54 CST 2014"), NoiseMode.CLEAN);
        record[30] = new EGVRecord(278, TrendArrow.FLAT, format.parse("Thu Dec 04 18:34:51 CST 2014"), format.parse("Fri Dec 05 00:34:54 CST 2014"), NoiseMode.LIGHT);
        record[31] = new EGVRecord(262, TrendArrow.FLAT, format.parse("Thu Dec 04 18:39:51 CST 2014"), format.parse("Fri Dec 05 00:39:54 CST 2014"), NoiseMode.LIGHT);
        record[32] = new EGVRecord(234, TrendArrow.DOUBLE_DOWN, format.parse("Thu Dec 04 18:44:51 CST 2014"), format.parse("Fri Dec 05 00:44:54 CST 2014"), NoiseMode.LIGHT);
        record[33] = new EGVRecord(155, TrendArrow.DOUBLE_DOWN, format.parse("Thu Dec 04 18:49:52 CST 2014"), format.parse("Fri Dec 05 00:49:54 CST 2014"), NoiseMode.CLEAN);
        record[34] = new EGVRecord(135, TrendArrow.DOUBLE_DOWN, format.parse("Thu Dec 04 18:54:51 CST 2014"), format.parse("Fri Dec 05 00:54:54 CST 2014"), NoiseMode.CLEAN);
        record[35] = new EGVRecord(122, TrendArrow.DOUBLE_DOWN, format.parse("Thu Dec 04 18:59:51 CST 2014"), format.parse("Fri Dec 05 00:59:54 CST 2014"), NoiseMode.CLEAN);
        record[36] = new EGVRecord(118, TrendArrow.DOUBLE_DOWN, format.parse("Thu Dec 04 19:04:53 CST 2014"), format.parse("Fri Dec 05 01:04:55 CST 2014"), NoiseMode.CLEAN);
        record[37] = new EGVRecord(121, TrendArrow.DOWN_45, format.parse("Thu Dec 04 19:09:52 CST 2014"), format.parse("Fri Dec 05 01:09:54 CST 2014"), NoiseMode.CLEAN);
        record[38] = new EGVRecord(127, TrendArrow.FLAT, format.parse("Thu Dec 04 19:14:52 CST 2014"), format.parse("Fri Dec 05 01:14:54 CST 2014"), NoiseMode.CLEAN);
        record[39] = new EGVRecord(137, TrendArrow.UP_45, format.parse("Thu Dec 04 19:19:52 CST 2014"), format.parse("Fri Dec 05 01:19:54 CST 2014"), NoiseMode.CLEAN);
        record[40] = new EGVRecord(157, TrendArrow.UP_45, format.parse("Thu Dec 04 19:34:52 CST 2014"), format.parse("Fri Dec 05 01:34:54 CST 2014"), NoiseMode.CLEAN);
        record[41] = new EGVRecord(158, TrendArrow.UP_45, format.parse("Thu Dec 04 19:39:52 CST 2014"), format.parse("Fri Dec 05 01:39:54 CST 2014"), NoiseMode.CLEAN);
        record[42] = new EGVRecord(164, TrendArrow.UP_45, format.parse("Thu Dec 04 19:44:52 CST 2014"), format.parse("Fri Dec 05 01:44:55 CST 2014"), NoiseMode.CLEAN);
        record[43] = new EGVRecord(164, TrendArrow.FLAT, format.parse("Thu Dec 04 19:49:51 CST 2014"), format.parse("Fri Dec 05 01:49:54 CST 2014"), NoiseMode.CLEAN);
        record[44] = new EGVRecord(166, TrendArrow.FLAT, format.parse("Thu Dec 04 19:54:51 CST 2014"), format.parse("Fri Dec 05 01:54:54 CST 2014"), NoiseMode.CLEAN);
        record[45] = new EGVRecord(174, TrendArrow.FLAT, format.parse("Thu Dec 04 19:59:51 CST 2014"), format.parse("Fri Dec 05 01:59:54 CST 2014"), NoiseMode.CLEAN);
        record[46] = new EGVRecord(167, TrendArrow.FLAT, format.parse("Thu Dec 04 20:04:51 CST 2014"), format.parse("Fri Dec 05 02:04:54 CST 2014"), NoiseMode.CLEAN);
        record[47] = new EGVRecord(170, TrendArrow.FLAT, format.parse("Thu Dec 04 20:09:51 CST 2014"), format.parse("Fri Dec 05 02:09:54 CST 2014"), NoiseMode.CLEAN);
        record[48] = new EGVRecord(169, TrendArrow.FLAT, format.parse("Thu Dec 04 20:14:51 CST 2014"), format.parse("Fri Dec 05 02:14:54 CST 2014"), NoiseMode.CLEAN);
        record[49] = new EGVRecord(168, TrendArrow.FLAT, format.parse("Thu Dec 04 20:19:51 CST 2014"), format.parse("Fri Dec 05 02:19:54 CST 2014"), NoiseMode.CLEAN);
        record[50] = new EGVRecord(166, TrendArrow.FLAT, format.parse("Thu Dec 04 20:24:51 CST 2014"), format.parse("Fri Dec 05 02:24:54 CST 2014"), NoiseMode.CLEAN);
        record[51] = new EGVRecord(166, TrendArrow.FLAT, format.parse("Thu Dec 04 20:29:51 CST 2014"), format.parse("Fri Dec 05 02:29:54 CST 2014"), NoiseMode.CLEAN);
        record[52] = new EGVRecord(161, TrendArrow.FLAT, format.parse("Thu Dec 04 20:34:51 CST 2014"), format.parse("Fri Dec 05 02:34:54 CST 2014"), NoiseMode.CLEAN);
        record[53] = new EGVRecord(171, TrendArrow.FLAT, format.parse("Thu Dec 04 20:39:51 CST 2014"), format.parse("Fri Dec 05 02:39:54 CST 2014"), NoiseMode.CLEAN);
        record[54] = new EGVRecord(166, TrendArrow.FLAT, format.parse("Thu Dec 04 20:44:51 CST 2014"), format.parse("Fri Dec 05 02:44:54 CST 2014"), NoiseMode.CLEAN);
        record[55] = new EGVRecord(173, TrendArrow.FLAT, format.parse("Thu Dec 04 20:49:51 CST 2014"), format.parse("Fri Dec 05 02:49:54 CST 2014"), NoiseMode.CLEAN);
        record[56] = new EGVRecord(178, TrendArrow.FLAT, format.parse("Thu Dec 04 20:54:51 CST 2014"), format.parse("Fri Dec 05 02:54:54 CST 2014"), NoiseMode.CLEAN);
        record[57] = new EGVRecord(181, TrendArrow.FLAT, format.parse("Thu Dec 04 21:04:52 CST 2014"), format.parse("Fri Dec 05 03:04:55 CST 2014"), NoiseMode.CLEAN);
        record[58] = new EGVRecord(181, TrendArrow.FLAT, format.parse("Thu Dec 04 21:04:52 CST 2014"), format.parse("Fri Dec 05 03:04:55 CST 2014"), NoiseMode.CLEAN);
        record[59] = new EGVRecord(186, TrendArrow.FLAT, format.parse("Thu Dec 04 21:09:53 CST 2014"), format.parse("Fri Dec 05 03:09:55 CST 2014"), NoiseMode.CLEAN);
        record[60] = new EGVRecord(186, TrendArrow.FLAT, format.parse("Thu Dec 04 21:09:53 CST 2014"), format.parse("Fri Dec 05 03:09:55 CST 2014"), NoiseMode.CLEAN);
        return record;
    }

    public static MeterRecord[] mockMeterPage() throws ParseException {
        DateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        MeterRecord[] record = new MeterRecord[10];
        record[0] = new MeterRecord(113, 186155018, format.parse("Tue Nov 25 07:44:05 CST 2014"), format.parse("Tue Nov 25 13:44:08 CST 2014"));
        record[1] = new MeterRecord(70, 186299694, format.parse("Wed Nov 26 23:55:21 CST 2014"), format.parse("Thu Nov 27 05:55:24 CST 2014"));
        record[2] = new MeterRecord(72, 186299717, format.parse("Wed Nov 26 23:55:44 CST 2014"), format.parse("Thu Nov 27 05:55:47 CST 2014"));
        record[3] = new MeterRecord(262, 186306654, format.parse("Thu Nov 27 01:51:22 CST 2014"), format.parse("Thu Nov 27 07:51:24 CST 2014"));
        record[4] = new MeterRecord(85, 186384870, format.parse("Thu Nov 27 23:34:58 CST 2014"), format.parse("Fri Nov 28 05:35:00 CST 2014"));
        record[5] = new MeterRecord(82, 186505003, format.parse("Sat Nov 29 08:57:11 CST 2014"), format.parse("Sat Nov 29 14:57:13 CST 2014"));
        record[6] = new MeterRecord(296, 186602003, format.parse("Sun Nov 30 11:53:50 CST 2014"), format.parse("Sun Nov 30 17:53:53 CST 2014"));
        record[7] = new MeterRecord(117, 186705166, format.parse("Mon Dec 01 16:33:14 CST 2014"), format.parse("Mon Dec 01 22:33:16 CST 2014"));
        record[8] = new MeterRecord(121, 186705180, format.parse("Mon Dec 01 16:33:27 CST 2014"), format.parse("Mon Dec 01 22:33:30 CST 2014"));
        record[9] = new MeterRecord(83, 186782153, format.parse("Tue Dec 02 13:56:21 CST 2014"), format.parse("Tue Dec 02 19:56:23 CST 2014"));
        return record;
    }

    public static SensorRecord[] mockSensorPage() throws ParseException {
        DateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        SensorRecord[] record = new SensorRecord[43];
        record[0] = new SensorRecord(146752, 149472,186, format.parse("Thu Dec 04 17:14:51 CST 2014"), format.parse("Thu Dec 04 23:14:54 CST 2014"));
        record[1] = new SensorRecord(148416, 151552,190, format.parse("Thu Dec 04 17:19:51 CST 2014"), format.parse("Thu Dec 04 23:19:54 CST 2014"));
        record[2] = new SensorRecord(150208, 153696,181, format.parse("Thu Dec 04 17:24:51 CST 2014"), format.parse("Thu Dec 04 23:24:54 CST 2014"));
        record[3] = new SensorRecord(152288, 155520,186, format.parse("Thu Dec 04 17:29:51 CST 2014"), format.parse("Thu Dec 04 23:29:54 CST 2014"));
        record[4] = new SensorRecord(154400, 156928,189, format.parse("Thu Dec 04 17:34:51 CST 2014"), format.parse("Thu Dec 04 23:34:54 CST 2014"));
        record[5] = new SensorRecord(156064, 156896,185, format.parse("Thu Dec 04 17:39:51 CST 2014"), format.parse("Thu Dec 04 23:39:54 CST 2014"));
        record[6] = new SensorRecord(156992, 156736,185, format.parse("Thu Dec 04 17:44:51 CST 2014"), format.parse("Thu Dec 04 23:44:54 CST 2014"));
        record[7] = new SensorRecord(159072, 165792,186, format.parse("Thu Dec 04 17:54:51 CST 2014"), format.parse("Thu Dec 04 23:54:54 CST 2014"));
        record[8] = new SensorRecord(162400, 171808,191, format.parse("Thu Dec 04 17:59:51 CST 2014"), format.parse("Thu Dec 04 23:59:54 CST 2014"));
        record[9] = new SensorRecord(167488, 175456,188, format.parse("Thu Dec 04 18:04:51 CST 2014"), format.parse("Fri Dec 05 00:04:54 CST 2014"));
        record[10] = new SensorRecord(177568, 185568,164, format.parse("Thu Dec 04 18:14:51 CST 2014"), format.parse("Fri Dec 05 00:14:54 CST 2014"));
        record[11] = new SensorRecord(182208, 192064,184, format.parse("Thu Dec 04 18:19:51 CST 2014"), format.parse("Fri Dec 05 00:19:54 CST 2014"));
        record[12] = new SensorRecord(187136, 193216,194, format.parse("Thu Dec 04 18:24:51 CST 2014"), format.parse("Fri Dec 05 00:24:53 CST 2014"));
        record[13] = new SensorRecord(190752, 185696,190, format.parse("Thu Dec 04 18:29:51 CST 2014"), format.parse("Fri Dec 05 00:29:53 CST 2014"));
        record[14] = new SensorRecord(189184, 167584,187, format.parse("Thu Dec 04 18:34:51 CST 2014"), format.parse("Fri Dec 05 00:34:53 CST 2014"));
        record[15] = new SensorRecord(179552, 149824,173, format.parse("Thu Dec 04 18:39:51 CST 2014"), format.parse("Fri Dec 05 00:39:53 CST 2014"));
        record[16] = new SensorRecord(162848, 132352,182, format.parse("Thu Dec 04 18:44:51 CST 2014"), format.parse("Fri Dec 05 00:44:53 CST 2014"));
        record[17] = new SensorRecord(142848, 114944,191, format.parse("Thu Dec 04 18:49:51 CST 2014"), format.parse("Fri Dec 05 00:49:53 CST 2014"));
        record[18] = new SensorRecord(123984, 102832,188, format.parse("Thu Dec 04 18:54:51 CST 2014"), format.parse("Fri Dec 05 00:54:53 CST 2014"));
        record[19] = new SensorRecord(108784, 94720,160, format.parse("Thu Dec 04 18:59:51 CST 2014"), format.parse("Fri Dec 05 00:59:53 CST 2014"));
        record[20] = new SensorRecord(98320, 92416,928, format.parse("Thu Dec 04 19:04:52 CST 2014"), format.parse("Fri Dec 05 01:04:55 CST 2014"));
        record[21] = new SensorRecord(92944, 94432,418, format.parse("Thu Dec 04 19:09:51 CST 2014"), format.parse("Fri Dec 05 01:09:54 CST 2014"));
        record[22] = new SensorRecord(92160, 97664,419, format.parse("Thu Dec 04 19:14:51 CST 2014"), format.parse("Fri Dec 05 01:14:54 CST 2014"));
        record[23] = new SensorRecord(94976, 103648,419, format.parse("Thu Dec 04 19:19:51 CST 2014"), format.parse("Fri Dec 05 01:19:54 CST 2014"));
        record[24] = new SensorRecord(111536, 116256,420, format.parse("Thu Dec 04 19:34:51 CST 2014"), format.parse("Fri Dec 05 01:34:54 CST 2014"));
        record[25] = new SensorRecord(115296, 116464,419, format.parse("Thu Dec 04 19:39:51 CST 2014"), format.parse("Fri Dec 05 01:39:54 CST 2014"));
        record[26] = new SensorRecord(117328, 119968,673, format.parse("Thu Dec 04 19:44:52 CST 2014"), format.parse("Fri Dec 05 01:44:54 CST 2014"));
        record[27] = new SensorRecord(118464, 120016,168, format.parse("Thu Dec 04 19:49:51 CST 2014"), format.parse("Fri Dec 05 01:49:53 CST 2014"));
        record[28] = new SensorRecord(119488, 121424,163, format.parse("Thu Dec 04 19:54:51 CST 2014"), format.parse("Fri Dec 05 01:54:53 CST 2014"));
        record[29] = new SensorRecord(121136, 126464,172, format.parse("Thu Dec 04 19:59:51 CST 2014"), format.parse("Fri Dec 05 01:59:53 CST 2014"));
        record[30] = new SensorRecord(122848, 122112,175, format.parse("Thu Dec 04 20:04:51 CST 2014"), format.parse("Fri Dec 05 02:04:53 CST 2014"));
        record[31] = new SensorRecord(124016, 124128,180, format.parse("Thu Dec 04 20:09:51 CST 2014"), format.parse("Fri Dec 05 02:09:53 CST 2014"));
        record[32] = new SensorRecord(124288, 123392,173, format.parse("Thu Dec 04 20:14:51 CST 2014"), format.parse("Fri Dec 05 02:14:53 CST 2014"));
        record[33] = new SensorRecord(123680, 122656,176, format.parse("Thu Dec 04 20:19:51 CST 2014"), format.parse("Fri Dec 05 02:19:53 CST 2014"));
        record[34] = new SensorRecord(122944, 121712,179, format.parse("Thu Dec 04 20:24:51 CST 2014"), format.parse("Fri Dec 05 02:24:53 CST 2014"));
        record[35] = new SensorRecord(122224, 121456,177, format.parse("Thu Dec 04 20:29:51 CST 2014"), format.parse("Fri Dec 05 02:29:53 CST 2014"));
        record[36] = new SensorRecord(121328, 118496,175, format.parse("Thu Dec 04 20:34:51 CST 2014"), format.parse("Fri Dec 05 02:34:53 CST 2014"));
        record[37] = new SensorRecord(120848, 124432,179, format.parse("Thu Dec 04 20:39:51 CST 2014"), format.parse("Fri Dec 05 02:39:53 CST 2014"));
        record[38] = new SensorRecord(121024, 121312,162, format.parse("Thu Dec 04 20:44:51 CST 2014"), format.parse("Fri Dec 05 02:44:53 CST 2014"));
        record[39] = new SensorRecord(122064, 125744,164, format.parse("Thu Dec 04 20:49:51 CST 2014"), format.parse("Fri Dec 05 02:49:53 CST 2014"));
        record[40] = new SensorRecord(124064, 128624,163, format.parse("Thu Dec 04 20:54:51 CST 2014"), format.parse("Fri Dec 05 02:54:53 CST 2014"));
        record[41] = new SensorRecord(128560, 130288,680, format.parse("Thu Dec 04 21:04:52 CST 2014"), format.parse("Fri Dec 05 03:04:54 CST 2014"));
        record[42] = new SensorRecord(130416, 133504,946, format.parse("Thu Dec 04 21:09:52 CST 2014"), format.parse("Fri Dec 05 03:09:55 CST 2014"));
        return record;
    }

    public static CalRecord[] mockCalPage() throws ParseException {
        DateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        CalRecord[] record = new CalRecord[2];
        CalSubrecord[] subrecord0 = new CalSubrecord[2];
        subrecord0[0] = new CalSubrecord(121, 70130, format.parse("Wed Mar 26 23:06:45 CDT 2014"), format.parse("Wed Mar 26 22:59:40 CDT 2014"));
        subrecord0[1] = new CalSubrecord(83, 74480, format.parse("Thu Mar 27 20:26:42 CDT 2014"), format.parse("Thu Mar 27 20:22:33 CDT 2014"));
        CalSubrecord[] subrecord1 = new CalSubrecord[2];
        subrecord1[0] = new CalSubrecord(121, 70130, format.parse("Wed Mar 26 23:06:45 CDT 2014"), format.parse("Wed Mar 26 22:59:40 CDT 2014"));
        subrecord1[1] = new CalSubrecord(83, 74480, format.parse("Thu Mar 27 20:26:42 CDT 2014"), format.parse("Thu Mar 27 20:22:33 CDT 2014"));
        record[0] = new CalRecord(17791.61320896296, 512.6837392183724, 0.9, 0.5, format.parse("Thu Dec 04 21:04:53 CST 2014"), format.parse("Fri Dec 05 03:04:55 CST 2014"), subrecord0);
        record[1] = new CalRecord(17791.61320896296, 512.6837392183724, 0.9, 0.5, format.parse("Thu Dec 04 21:09:53 CST 2014"), format.parse("Fri Dec 05 03:09:55 CST 2014"), subrecord1);
        return record;
    }

}
