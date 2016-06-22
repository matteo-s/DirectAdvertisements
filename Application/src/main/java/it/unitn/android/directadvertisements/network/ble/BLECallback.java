/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.ble;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

public class BLECallback extends AdvertiseCallback {
    private ActionListener mListener;

    public BLECallback(ActionListener listener) {
        mListener = listener;
    }


    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        Log.v("BLECallback", "start advertising");

        //signal
        mListener.onSuccess();

        //call super
        super.onStartSuccess(settingsInEffect);
    }

    @Override
    public void onStartFailure(int errorCode) {
        Log.v("BLECallback", "start advertising failure " + String.valueOf(errorCode));

        //signal
        mListener.onFailure(errorCode);

        //call super
        super.onStartFailure(errorCode);
    }

}
