/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.wifi;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import it.unitn.android.directadvertisements.network.NetworkMessage;

public class WifiNetworkMessage extends NetworkMessage {


    public static final int SLOTS = 10;

    public WifiNetworkMessage() {
        sender = 0;
        clock = 0;
        address = "";

        clocks = new HashMap<>();

        addresses = new HashMap<>();
    }

    /*
    * Helpers
     */

    public Map<String, String> getRecord() {
        //build data - 200bytes max
        Map<String, String> data = new LinkedHashMap<String, String>();
        data.put("s", Integer.toString(sender));
//        data.put("c", Short.toString(clock));

        //add all nodes as vector via stringBuilder
        StringBuilder sb = new StringBuilder();

        for (int i = 1; i <= SLOTS; i++) {
            short c = 0;
            if (clocks.containsKey(i)) {
                c = clocks.get(i);
            }
            //add sender
            if (i == sender) {
                c = clock;
            }

            //char 16bit = short
            //char is unsigned, short is signed but clock is >0
            sb.append((char) c);
//            sb.append(String.valueOf(c));
        }

        data.put("v", sb.toString());

        return data;
    }






    /*
    * Factory
     */

    public static WifiNetworkMessage parse(NetworkMessage msg) {
        WifiNetworkMessage m = new WifiNetworkMessage();
        //clone data
        m.sender = msg.sender;
        m.clock = msg.clock;

        m.clocks = msg.clocks;
        m.addresses = msg.addresses;

        //return
        return m;

    }


    public static WifiNetworkMessage parse(Map<String, String> record) {
        WifiNetworkMessage m = null;

        if (record.containsKey("s")
                && record.containsKey("c")
                && record.containsKey("v")) {
            //create
            m = new WifiNetworkMessage();

            m.sender = Integer.parseInt(record.get("s"));
//            m.clock = Short.parseShort(record.get("c"));

            //get vector
            String v = record.get("v");

             for (int i = 0; i < SLOTS; i++) {
                 //char is unsigned, short is signed but clock is >0
                 char s = v.charAt(i);
                 short c = (short)s;

                 //clock should be > 0 if present
                 if (c > 0) {
                     m.clocks.put(i, c);

                     if (i == m.sender) {
                         m.clock = c;
                     }
                 }
             }



        }


        //return
        return m;


    }


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

}
