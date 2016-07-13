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
        String reason = "UNKNOWN";
        //from AdvertiseCallback
        switch (errorCode) {
            case 1:
                reason = "ADVERTISE_FAILED_DATA_TOO_LARGE";
                break;
            case 2:
                reason = "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS";
                break;
            case 3:
                reason = "ADVERTISE_FAILED_ALREADY_STARTED";
                break;
            case 4:
                reason = "ADVERTISE_FAILED_INTERNAL_ERROR";
                break;
            case 5:
                reason = "ADVERTISE_FAILED_FEATURE_UNSUPPORTED";
                break;
        }
        Log.v("BLECallback", "start advertising failure " + String.valueOf(errorCode) + ": " + reason);

        //signal
        mListener.onFailure(errorCode);

        //call super
        super.onStartFailure(errorCode);
    }

}
