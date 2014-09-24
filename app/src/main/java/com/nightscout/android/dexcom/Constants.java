package com.nightscout.android.dexcom;

import com.nightscout.android.protobuf.SGV;

public class Constants {

    public final static int NULL = 0;
    public final static int ACK = 1;
    public final static int NAK = 2;
    public final static int INVALID_COMMAND = 3;
    public final static int INVALID_PARAM = 4;
    public final static int INCOMPLETE_PACKET_RECEIVED = 5;
    public final static int RECEIVER_ERROR = 6;
    public final static int INVALID_MODE = 7;
    public final static int PING = 10;
    public final static int READ_FIRMWARE_HEADER = 11;
    public final static int READ_DATABASE_PARTITION_INFO = 15;
    public final static int READ_DATABASE_PAGE_RANGE = 16;
    public final static int READ_DATABASE_PAGES = 17;
    public final static int READ_DATABASE_PAGE_HEADER = 18;
    public final static int READ_TRANSMITTER_ID = 25;
    public final static int WRITE_TRANSMITTER_ID = 26;
    public final static int READ_LANGUAGE = 27;
    public final static int WRITE_LANGUAGE = 28;
    public final static int READ_DISPLAY_TIME_OFFSET = 29;
    public final static int WRITE_DISPLAY_TIME_OFFSET = 30;
    public final static int READ_RTC = 31;
    public final static int RESET_RECEIVER = 32;
    public final static int READ_BATTERY_LEVEL = 33;
    public final static int READ_SYSTEM_TIME = 34;
    public final static int READ_SYSTEM_TIME_OFFSET = 35;
    public final static int WRITE_SYSTEM_TIME = 36;
    public final static int READ_GLUCOSE_UNIT = 37;
    public final static int WRITE_GLUCOSE_UNIT = 38;
    public final static int READ_BLINDED_MODE = 39;
    public final static int WRITE_BLINDED_MODE = 40;
    public final static int READ_CLOCK_MODE = 41;
    public final static int WRITE_CLOCK_MODE = 42;
    public final static int READ_DEVICE_MODE = 43;
    public final static int ERASE_DATABASE = 45;
    public final static int SHUTDOWN_RECEIVER = 46;
    public final static int WRITE_PC_PARAMETERS = 47;
    public final static int READ_BATTERY_STATE = 48;
    public final static int READ_HARDWARE_BOARD_ID = 49;
    public final static int READ_FIRMWARE_SETTINGS = 54;
    public final static int READ_ENABLE_SETUP_WIZARD_FLAG = 55;
    public final static int READ_SETUP_WIZARD_STATE = 57;
    public final static int MAX_COMMAND = 59;
    public final static int MAX_POSSIBLE_COMMAND = 255;
    public final static int EGV_VALUE_MASK = 1023;
    public final static int EGV_DISPLAY_ONLY_MASK = 32768;
    public final static int EGV_TREND_ARROW_MASK = 15;
    public final static int EGV_NOISE_MASK = 112;

    public enum BATTERY_STATES {
        NONE,
        CHARGING,
        NOT_CHARGING,
        NTC_FAULT,
        BAD_BATTERY
    }

    public enum RECORD_TYPES {
        MANUFACTURING_DATA,
        FIRMWARE_PARAMETER_DATA,
        PC_SOFTWARE_PARAMETER,
        SENSOR_DATA,
        EGV_DATA,
        CAL_SET,
        DEVIATION,
        INSERTION_TIME,
        RECEIVER_LOG_DATA,
        RECEIVER_ERROR_DATA,
        METER_DATA,
        USER_EVENT_DATA,
        USER_SETTING_DATA,
        MAX_VALUE
    }

    public enum TREND_ARROW_VALUES {
        NONE(SGV.CookieMonsterSGVG4.Direction.NONE),
        DOUBLE_UP("\u21C8", "DoubleUp",SGV.CookieMonsterSGVG4.Direction.DOUBLE_UP),
        SINGLE_UP("\u2191", "SingleUp",SGV.CookieMonsterSGVG4.Direction.SINGLE_UP),
        UP_45("\u2197", "FortyFiveUp",SGV.CookieMonsterSGVG4.Direction.FORTY_FIVE_UP),
        FLAT("\u2192", "Flat",SGV.CookieMonsterSGVG4.Direction.FLAT),
        DOWN_45("\u2198", "FortyFiveDown",SGV.CookieMonsterSGVG4.Direction.FORTY_FIVE_DOWN),
        SINGLE_DOWN("\u2193", "SingleDown",SGV.CookieMonsterSGVG4.Direction.SINGLE_DOWN),
        DOUBLE_DOWN("\u21CA", "DoubleDown",SGV.CookieMonsterSGVG4.Direction.DOUBLE_DOWN),
        NOT_COMPUTABLE(SGV.CookieMonsterSGVG4.Direction.NOT_COMPUTABLE),
        OUT_OF_RANGE(SGV.CookieMonsterSGVG4.Direction.RATE_OUT_OF_RANGE);

        private String arrowSymbol;
        private String trendName;
        private SGV.CookieMonsterSGVG4.Direction protoBuffEnum;

        TREND_ARROW_VALUES(String a, String t, SGV.CookieMonsterSGVG4.Direction d) {
            arrowSymbol = a;
            trendName = t;
            protoBuffEnum = d;
        }

        TREND_ARROW_VALUES(SGV.CookieMonsterSGVG4.Direction d) {
            this(null, null,d);
        }

        public String Symbol() {
            if (arrowSymbol == null) {
                return "\u2194";
            } else {
                return arrowSymbol;
            }
        }

        public String friendlyTrendName() {
            if (trendName == null) {
                return this.name().replace("_", " ");
            } else {
                return this.trendName;
            }
        }

        public SGV.CookieMonsterSGVG4.Direction getProtoBuffEnum(){
            return this.protoBuffEnum;
        }

    }

}
