package com.nightscout.android.drivers;

import java.util.UUID;

public class DexShareAttributes {

    //Share Service String
    public static final UUID CradleService = UUID.fromString("F0ACA0B1-EBFA-F96F-28DA-076C35A521DB");

    //Share Characteristic Strings
    public static final UUID AuthenticationCode = UUID.fromString("F0ACACAC-EBFA-F96F-28DA-076C35A521DB");
    public static final UUID ShareMessageReceiver = UUID.fromString("F0ACB20A-EBFA-F96F-28DA-076C35A521DB"); // Max 20 Bytes - Writable
    public static final UUID ShareMessageResponse = UUID.fromString("F0ACB20B-EBFA-F96F-28DA-076C35A521DB"); // Max 20 Bytes
    public static final UUID Command = UUID.fromString("F0ACB0CC-EBFA-F96F-28DA-076C35A521DB");
    public static final UUID Response = UUID.fromString("F0ACB0CD-EBFA-F96F-28DA-076C35A521DB"); // Writable?
    public static final UUID HeartBeat = UUID.fromString("F0AC2B18-EBFA-F96F-28DA-076C35A521DB");
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";


/*
charUuid=f0ac2b18-ebfa-f96f-28da-076c35a521db, prop=18
charUuid=f0acb20a-ebfa-f96f-28da-076c35a521db, prop=10
charUuid=f0acb20b-ebfa-f96f-28da-076c35a521db, prop=34
charUuid=f0acacac-ebfa-f96f-28da-076c35a521db, prop=10
charUuid=f0acb0cc-ebfa-f96f-28da-076c35a521db, prop=10
charUuid=f0acb0cd-ebfa-f96f-28da-076c35a521db, prop=42
charUuid=f0acb0cd-ebfa-f96f-28da-076c35a521db, prop=42

12:0e:00:db:21:a5:35:6c:07:da:28:6f:f9:fa:eb: 18:2b:ac:f0 handle:0d
0a:12:00:db:21:a5:35:6c:07:da:28:6f:f9:fa:eb: 0a:b2:ac:f0 handle:11
22:14:00:db:21:a5:35:6c:07:da:28:6f:f9:fa:eb: 0b:b2:ac:f0 handle:13
0a:17:00:db:21:a5:35:6c:07:da:28:6f:f9:fa:eb: ac:ac:ac:f0 handle:16
0a:19:00:db:21:a5:35:6c:07:da:28:6f:f9:fa:eb: cc:b0:ac:f0 handle:18
2a:1b:00:db:21:a5:35:6c:07:da:28:6f:f9:fa:eb: cd:b0:ac:f0 handle:1a

Unknown Service (f0aca0b1-ebfa-f96f-28da-076c35a521db)
- Unknown Characteristic [N R] (f0ac2b18-ebfa-f96f-28da-076c35a521db)
   Client Characteristic Configuration (0x2902)
   Characteristic Presentation Format (0x2904)
- Unknown Characteristic [R W] (f0acb20a-ebfa-f96f-28da-076c35a521db)
- Unknown Characteristic [I R] (f0acb20b-ebfa-f96f-28da-076c35a521db)
   Client Characteristic Configuration (0x2902)
- Unknown Characteristic [R W] (f0acacac-ebfa-f96f-28da-076c35a521db)
- Unknown Characteristic [R W] (f0acb0cc-ebfa-f96f-28da-076c35a521db)
- Unknown Characteristic [I R W] (f0acb0cd-ebfa-f96f-28da-076c35a521db)
   Client Characteristic Configuration (0x2902)


 */


    //Device Info
    public static final UUID DeviceService = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID PowerLevel = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");


//

    // Message Structure - 20 Bytes Total
//    Uchar messageNumber
//    Uchar totalMessages
//    Uchar messagegBytes[18]

//    Write a message to the ShareMessageReceiver using the message structure defined above

//    public static UUID uuid_for(String string) {
//        String s2 = string.replace("-", "");
//        UUID uuid = new UUID(new BigInteger(s2.substring(0, 16), 16).longValue(), new BigInteger(s2.substring(16), 16).longValue());
//        return uuid;
//    }
}
