/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.wifi;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import it.unitn.android.directadvertisements.network.NetworkMessage;

public class WifiNetworkMessage extends NetworkMessage {


    /*
    * Helpers
     */

    public Map<String, String> getRecord() {
        //build data - 200bytes max
        Map<String, String> data = new LinkedHashMap<String, String>();
//        data.put("s", sender);
        data.put("0", Short.toString(clock));

        //add all nodes - max
//        for (String a : clocks.keySet()) {
//            if (addresses.containsKey(a)) {
//                byte i = addresses.get(a);
//                short c = clocks.get(a);
//
//                //add as id - clock
//                data.put(Short.toString(i), Short.toString(c));
//
//            }
//        }

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
