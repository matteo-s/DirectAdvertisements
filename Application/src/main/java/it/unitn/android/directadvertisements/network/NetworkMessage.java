/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network;

import java.io.Serializable;
import java.util.Map;

public class NetworkMessage implements Serializable {

    //local node info
    public int sender;
    public short clock;
    public String address;

    //maps device id - clock
    public Map<Integer, Short> clocks;

    //maps device id - address
    public Map<Integer, String> addresses;

}
