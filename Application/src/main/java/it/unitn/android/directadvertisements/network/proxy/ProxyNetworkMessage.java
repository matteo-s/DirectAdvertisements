/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.proxy;

import java.util.HashMap;

import it.unitn.android.directadvertisements.network.NetworkMessage;

public class ProxyNetworkMessage extends NetworkMessage {

    public static final int SLOTS = 10;

    public ProxyNetworkMessage() {
        sender = 0;
        clock = 0;
        address = "";

        clocks = new HashMap<>();

        addresses = new HashMap<>();
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

    public static String byteArrayToString(byte[] ba) {
        StringBuilder hex = new StringBuilder(ba.length * 2);
        for (byte b : ba)
            hex.append(b + " ");
        return hex.toString();
    }
}
