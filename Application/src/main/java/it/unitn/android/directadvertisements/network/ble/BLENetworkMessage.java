/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.ble;

import android.bluetooth.le.AdvertiseData;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import it.unitn.android.directadvertisements.network.NetworkMessage;

public class BLENetworkMessage extends NetworkMessage {

    public static final int SLOTS = 18;

    public BLENetworkMessage() {
        sender = 0;
        clock = 0;
        address = "";

        clocks = new HashMap<>();

        addresses = new HashMap<>();
    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    public AdvertiseData buildServiceData() {

        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         *  This includes everything put into AdvertiseData including UUIDs, device info, &
         *  arbitrary service or manufacturer data.
         *  Attempting to send packets over this limit will result in a failure with error code
         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         *  onStartFailure() method of an AdvertiseCallback implementation.
         */

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeTxPowerLevel(true);
        dataBuilder.setIncludeDeviceName(false);

        //disable service uuid, rely on manufacturer data
//        dataBuilder.addServiceUuid(BLENetworkService.Service_UUID);


        /* For example - this will cause advertising to fail (exceeds size limit) */
        //String failureData = "asdghkajsghalkxcjhfa;sghtalksjcfhalskfjhasldkjfhdskf";
        //dataBuilder.addServiceData(Constants.Service_UUID, failureData.getBytes());

        //
        //declare a fixed length payload
//        int length = 3 + (2 * SLOTS);
        int length = 1 + (1 * SLOTS);
        byte[] bytes = new byte[length];

        //consider id as byte - limits to 127 ids
        //could use masks to convert to 4 bytes
//        data[0] = (byte) width;
//        data[1] = (byte) (width >>> 8);
//        data[2] = (byte) (width >>> 16);
//        data[3] = (byte) (width >>> 24);
        byte s = (byte) sender;

        //add as 0
        bytes[0] = s;


        //store clock as 2 bytes - disabled due to space limit
//        bytes[1] = (byte) clock;
//        bytes[2] = (byte) (clock >>> 8);


        //remaining space allocated to slots of 1 bytes
//        int k = 3;
        int k = 1;
        for (int i = 1; i <= SLOTS; i++) {
            short c = 0;
            if (clocks.containsKey(i)) {
                c = clocks.get(i);
            }
            //add sender
            if (i == sender) {
                c = clock;
            }

            //store as 1 bytes
            //java byte is signed, need to read with mask at receive side
            bytes[k] = (byte) c;

            //increment
            k = k + 1;
        }

        //build on service data
        dataBuilder.addServiceData(BLENetworkService.Service_UUID, bytes);
        return dataBuilder.build();

    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    public AdvertiseData buildManufacturerData() {

        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         *  This includes everything put into AdvertiseData including UUIDs, device info, &
         *  arbitrary service or manufacturer data.
         *  Attempting to send packets over this limit will result in a failure with error code
         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         *  onStartFailure() method of an AdvertiseCallback implementation.
         */

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeTxPowerLevel(true);
        dataBuilder.setIncludeDeviceName(false);

        byte[] bytes = buildManufacturerBytes();

        //add as manufacturer - id used is Google's one
        dataBuilder.addManufacturerData(224, bytes);
        return dataBuilder.build();
    }

    public byte[] buildManufacturerBytes() {
        //build on manufacturer data - use 24 bytes
        /*
        * Structure of packet
        *  | 0 1  | 2  | 3 --- 23 |
        *  | UUID | id | clocks   |
         */
        int length = 2 + 1 + (1 * SLOTS);
        byte[] bytes = new byte[length];

        //use 2 bytes as identifier: D1AD = D1rect ADvertisement
        bytes[0] = (byte) 0xD1; // Beacon Identifier
        bytes[1] = (byte) 0xAD; // Beacon Identifier


        //consider id as byte - limits to 127 ids
        //could use masks to convert to 4 bytes
//        data[0] = (byte) width;
//        data[1] = (byte) (width >>> 8);
//        data[2] = (byte) (width >>> 16);
//        data[3] = (byte) (width >>> 24);
        byte s = (byte) sender;

        //add as 2
        bytes[2] = s;


        //store clock as 2 bytes - disabled due to space limit
//        bytes[1] = (byte) clock;
//        bytes[2] = (byte) (clock >>> 8);


        //remaining space allocated to slots of 1 bytes
        int k = 3;
        for (int i = 1; i <= SLOTS; i++) {
            short c = 0;
            if (clocks.containsKey(i)) {
                c = clocks.get(i);
            }
            //add sender
            if (i == sender) {
                c = clock;
            }

            //store as 1 bytes
            //java byte is signed, need to read with mask at receive side
            bytes[k] = (byte) c;

            //increment
            k = k + 1;
        }

        return bytes;
    }

    public byte[] buildTemplateData() {

        //build on manufacturer data - use 24 bytes max
        /*
        * Structure of packet
        *  | 0 1  | 2  | 3 --- 23 |
        *  | UUID | id | clocks   |
         */
        int length = 2 + 1 + (1 * SLOTS);
        byte[] bytes = new byte[length];

        //use 2 bytes as identifier: D1AD = D1rect ADvertisement
        bytes[0] = (byte) 0xD1; // Beacon Identifier
        bytes[1] = (byte) 0xAD; // Beacon Identifier


        //consider id as byte - limits to 127 ids
        //could use masks to convert to 4 bytes
//        data[0] = (byte) width;
//        data[1] = (byte) (width >>> 8);
//        data[2] = (byte) (width >>> 16);
//        data[3] = (byte) (width >>> 24);
        byte s = (byte) sender;

        //add as 2
        bytes[2] = s;


        //store clock as 2 bytes - disabled due to space limit
//        bytes[1] = (byte) clock;
//        bytes[2] = (byte) (clock >>> 8);


        //remaining space allocated to slots of 1 bytes
        int k = 3;
        for (int i = 1; i <= SLOTS; i++) {
            short c = 0;
            if (clocks.containsKey(i)) {
                c = clocks.get(i);
            }
            //add sender
            if (i == sender) {
                c = clock;
            }

            //store as 1 bytes
            //java byte is signed, need to read with mask at receive side
            bytes[k] = (byte) c;

            //increment
            k = k + 1;
        }

        return bytes;
    }

//    /**
//     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
//     */
//    public AdvertiseData buildAdvertiseData() {
//
//        /**
//         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
//         *  This includes everything put into AdvertiseData including UUIDs, device info, &
//         *  arbitrary service or manufacturer data.
//         *  Attempting to send packets over this limit will result in a failure with error code
//         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
//         *  onStartFailure() method of an AdvertiseCallback implementation.
//         */
//
//        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
//        dataBuilder.addServiceUuid(BLENetworkService.Service_UUID);
//        dataBuilder.setIncludeDeviceName(true);
//
//        /* For example - this will cause advertising to fail (exceeds size limit) */
//        //String failureData = "asdghkajsghalkxcjhfa;sghtalksjcfhalskfjhasldkjfhdskf";
//        //dataBuilder.addServiceData(Constants.Service_UUID, failureData.getBytes());
//
//        //
//        //declare a fixed length payload
////        int length = 3 + (2 * SLOTS);
//        int length = 1 + (2 * SLOTS);
//        byte[] bytes = new byte[length];
//
//        //consider id as byte - limits to 127 ids
//        //could use masks to convert to 4 bytes
////        data[0] = (byte) width;
////        data[1] = (byte) (width >>> 8);
////        data[2] = (byte) (width >>> 16);
////        data[3] = (byte) (width >>> 24);
//        byte s = (byte) sender;
//
//        //add as 0
//        bytes[0] = s;
//
//
//        //store clock as 2 bytes - disabled due to space limit
////        bytes[1] = (byte) clock;
////        bytes[2] = (byte) (clock >>> 8);
//
//
//        //remaining space allocated to slots of 2 bytes
////        int k = 3;
//        int k = 1;
//        for (int i = 1; i <= SLOTS; i++) {
//            short c = 0;
//            if (clocks.containsKey(i)) {
//                c = clocks.get(i);
//            }
//            //add sender
//            if (i == sender) {
//                c = clock;
//            }
//
//            //store as 2 bytes
//            bytes[k] = (byte) c;
//            bytes[k + 1] = (byte) (c >>> 8);
//
//            //increment
//            k = k + 2;
//        }
//
//        //build
//        dataBuilder.addServiceData(BLENetworkService.Service_UUID, bytes);
////        dataBuilder.addServiceData(BLENetworkService.Service_UUID, new byte[5]);
//        return dataBuilder.build();
//    }

   /*
    * Factory
     */

    public static BLENetworkMessage parse(NetworkMessage msg) {
        BLENetworkMessage m = new BLENetworkMessage();
        //clone data
        m.sender = msg.sender;
        m.clock = msg.clock;

        m.clocks = msg.clocks;
        m.addresses = msg.addresses;

        //return
        return m;

    }

    public static BLENetworkMessage parseServiceData(byte[] bytes) {
        BLENetworkMessage m = null;

        //check length
//        int length = 3 + (2 * SLOTS);
        int length = 1 + (1 * SLOTS);
        if (bytes.length == length) {
            //create
            m = new BLENetworkMessage();

            //parse id
            m.sender = bytes[0];

//            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(2);

//            //parse clock from 2 bytes - disabled
//            byteBuffer.put(bytes[1]);
//            byteBuffer.put(bytes[2]);
//
//            byteBuffer.flip();
//
//            m.clock = byteBuffer.getShort();
//
//            byteBuffer.clear();


            //remaining space allocated to slots of 1 bytes
//            int k = 3;
            int k = 1;
            for (int i = 1; (i <= SLOTS && k < bytes.length); i++) {
//                byteBuffer.put(bytes[k]);
//                byteBuffer.put(bytes[k + 1]);
//
//                byteBuffer.flip();
//
//                short c = byteBuffer.getShort();

                short c = (short) (bytes[k] & 0xFF);
                //clock should be > 0 if present
                if (c > 0) {
                    m.clocks.put(i, c);

                    if (i == m.sender) {
                        m.clock = c;
                    }
                }

//                byteBuffer.clear();

                //increment
                k = k + 1;
            }

        }


        //return
        return m;

    }


    public static BLENetworkMessage parseManufacturerData(byte[] bytes) {
        BLENetworkMessage m = null;

      /*
        * Structure of packet
        *  | 0 1  | 2  | 3 --- 23 |
        *  | UUID | id | clocks   |
         */

        //check length
        int length = 2 + 1 + (1 * SLOTS);
        if (bytes.length >= length) {
            //create
            m = new BLENetworkMessage();

            //check service uuid
            byte id0 = bytes[0];
            byte id1 = bytes[1];

            if (id0 == (byte) 0xD1 && id1 == (byte) 0xAD) {

                //parse sender
                m.sender = bytes[2];

                //remaining space allocated to slots of 1 bytes
                int k = 3;
                for (int i = 1; (i <= SLOTS && k < bytes.length); i++) {
                    //use mask because byte is signed
                    short c = (short) (bytes[k] & 0xFF);
                    //clock should be > 0 if real value
                    if (c > 0) {
                        m.clocks.put(i, c);

                        if (i == m.sender) {
                            m.clock = c;
                        }
                    }

                    //increment
                    k = k + 1;
                }
            }
        }


        //return
        return m;

    }
//    public static BLENetworkMessage parse(byte[] bytes) {
//        BLENetworkMessage m = null;
//
//        //check length
////        int length = 3 + (2 * SLOTS);
//        int length = 1 + (2 * SLOTS);
//        if (bytes.length == length) {
//            //create
//            m = new BLENetworkMessage();
//
//            //parse id
//            m.sender = bytes[0];
//
////            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(2);
//
////            //parse clock from 2 bytes - disabled
////            byteBuffer.put(bytes[1]);
////            byteBuffer.put(bytes[2]);
////
////            byteBuffer.flip();
////
////            m.clock = byteBuffer.getShort();
////
////            byteBuffer.clear();
//
//
//            //remaining space allocated to slots of 2 bytes
////            int k = 3;
//            int k = 1;
//            for (int i = 1; (i <= SLOTS && k < bytes.length); i++) {
////                byteBuffer.put(bytes[k]);
////                byteBuffer.put(bytes[k + 1]);
////
////                byteBuffer.flip();
////
////                short c = byteBuffer.getShort();
//
//                short c = (short) ((bytes[k + 1] << 8) + (bytes[k] & 0xFF));
//                //clock should be > 0 if present
//                if (c > 0) {
//                    m.clocks.put(i, c);
//
//                    if (i == m.sender) {
//                        m.clock = c;
//                    }
//                }
//
////                byteBuffer.clear();
//
//                //increment
//                k = k + 2;
//            }
//
//        }
//
//
//        //return
//        return m;
//
//    }


    /*
* Converters
 */
    public byte[] macToByte(String macAddress) {
        String[] macAddressParts = macAddress.split(":");

        // convert hex string to byte values
        byte[] macAddressBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            Integer hex = Integer.parseInt(macAddressParts[i], 16);
            macAddressBytes[i] = hex.byteValue();
        }
        return macAddressBytes;
    }

    public String byteToMac(byte[] macAddressBytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            byte b = macAddressBytes[i];
            sb.append(String.format("%02x", b & 0xff));
            if (i < 5) {
                sb.append(":");
            }
        }

        return sb.toString();
    }

    public static String byteArrayToString(byte[] ba) {
        StringBuilder hex = new StringBuilder(ba.length * 2);
        for (byte b : ba)
            hex.append(b + " ");
        return hex.toString();
    }
}
