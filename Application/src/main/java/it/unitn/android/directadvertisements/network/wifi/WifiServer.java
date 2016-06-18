/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.wifi;

import android.os.Handler;
import android.os.Messenger;

import java.net.InetAddress;

public class WifiServer {
    private Messenger mMessenger;
    private Handler mHandler;
    private int mPort;

    public WifiServer(int port, Messenger messenger) {
        mPort = port;
        mMessenger = messenger;

        //create an handler for delayed tasks
        mHandler = new Handler();
    }


    public void listen() {

    }


    public void close() {

    }

}
