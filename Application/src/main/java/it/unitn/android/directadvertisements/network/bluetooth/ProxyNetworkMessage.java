/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.bluetooth;

import java.util.HashMap;

import it.unitn.android.directadvertisements.network.NetworkMessage;
import it.unitn.android.directadvertisements.network.ble.BLENetworkMessage;

public class ProxyNetworkMessage extends NetworkMessage {


    public static final int SLOTS = BLENetworkMessage.SLOTS;

    public ProxyNetworkMessage() {
        sender = 0;
        clock = 0;
        address = "";

        clocks = new HashMap<>();

        addresses = new HashMap<>();

    }

    public static ProxyNetworkMessage parse(NetworkMessage msg) {
        ProxyNetworkMessage m = new ProxyNetworkMessage();
        //clone data
        m.sender = msg.sender;
        m.clock = msg.clock;

        m.clocks = msg.clocks;
        m.addresses = msg.addresses;

        //return
        return m;

    }

    public byte[] buildData() {
        //use ble
        BLENetworkMessage bleMessage = new BLENetworkMessage();
        bleMessage.sender = this.sender;
        bleMessage.clock = this.clock;
        bleMessage.address = this.address;
        bleMessage.addresses = this.addresses;
        bleMessage.clocks = this.clocks;


        return bleMessage.buildManufacturerBytes();

    }


    public static ProxyNetworkMessage parseData(byte[] bytes) {
        BLENetworkMessage bleMessage = BLENetworkMessage.parseManufacturerData(bytes);

        ProxyNetworkMessage m = new ProxyNetworkMessage();
        //clone data
        m.sender = bleMessage.sender;
        m.clock = bleMessage.clock;

        m.clocks = bleMessage.clocks;
        m.addresses = bleMessage.addresses;

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

    public static String byteArrayToString(byte[] ba) {
        StringBuilder hex = new StringBuilder(ba.length * 2);
        for (byte b : ba)
            hex.append(b + " ");
        return hex.toString();
    }
}
