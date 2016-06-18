/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.wifi;

import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.os.Handler;
import android.os.Messenger;
import android.util.Log;

import java.util.Map;

public class WifiAdvertiser {


    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private Messenger mMessenger;
    private WifiP2pDnsSdServiceInfo mInfo;

    private boolean isActive = false;
    private Handler mHandler;

    public WifiAdvertiser(WifiP2pManager manager, WifiP2pManager.Channel channel, Messenger messenger) {
        mManager = manager;
        mChannel = channel;
        mMessenger = messenger;
        mInfo = null;

        //create an handler for delayed tasks
        mHandler = new Handler();
    }

    /*
    * Advertiser
    * callback listener will be called after start+duration+stop cycle
     */


    public void advertise(final WifiNetworkMessage m, final int duration, final WifiP2pManager.ActionListener listener) {
        Log.v("WifiAdvertiser", "advertise for " + String.valueOf(duration));

        if (isActive) {
            //call stop with callback
            stop(new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    //call advertise now
                    advertise(m, duration, listener);
                }

                @Override
                public void onFailure(int error) {
                    //call listener
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                }
            });
        } else {

            Map<String, String> record = m.getRecord();

            //add txt visibility key
            record.put("available", "visible");
            Log.v("WifiAdvertiser", "advertise data " + record.keySet().toString() + ": " + record.values().toString());

            //start
            start(record, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    isActive = true;
                    Log.v("WifiAdvertiser", "start advertise");

                    //use handler for stopping after duration - 1 shot
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("WifiAdvertiser", "advertiser expired");
                            stop(listener);
                        }
                    }, duration);

                }

                @Override
                public void onFailure(int error) {
                    Log.v("WifiAdvertiser", "fail advertise");
                    //call listener
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                }
            });
        }
    }


    public void start(Map<String, String> record, final WifiP2pManager.ActionListener listener) {
        if (isActive) {
            stop(null);
        }

        //add txt visibility key
//        record.put("available", "visible");

        mInfo = WifiP2pDnsSdServiceInfo.newInstance(
                WifiNetworkService.SERVICE_INSTANCE, WifiNetworkService.SERVICE_REG_TYPE, record);

        mManager.addLocalService(mChannel, mInfo, listener);
    }


    public void stop(final WifiP2pManager.ActionListener listener) {
        if (isActive && mInfo != null) {
            //stop
            mManager.removeLocalService(mChannel, mInfo, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    isActive = false;
                    mInfo = null;

                    Log.v("WifiAdvertiser", "stop advertise");

                    //call listener
                    if (listener != null) {
                        listener.onSuccess();
                    }
                }

                @Override
                public void onFailure(int error) {
                    Log.v("WifiAdvertiser", "fail advertise");
                    //call listener
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                }
            });
        }
    }

}
