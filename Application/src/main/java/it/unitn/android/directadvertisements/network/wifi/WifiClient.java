/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.wifi;

import android.os.Handler;
import android.os.Messenger;

import java.net.InetAddress;

public class WifiClient {

    private Messenger mMessenger;
    private Handler mHandler;
    private InetAddress mAddress;

    public WifiClient(InetAddress address, Messenger messenger) {
        mAddress = address;
        mMessenger = messenger;

        //create an handler for delayed tasks
        mHandler = new Handler();
    }

    public void connect() {

    }

    public void close() {

    }

    public void send() {

    }


}
