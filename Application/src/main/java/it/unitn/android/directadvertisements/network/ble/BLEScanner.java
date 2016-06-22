/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.Messenger;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BLEScanner {


    private BluetoothAdapter mAdapter;
    private BluetoothLeScanner mScanner;
    private ScanFilter mScanFilter;
    private ScanSettings mSettings;

    private BLEReceiver mReceiver;

    private Messenger mMessenger;

    private boolean isActive = false;
    private Handler mHandler;


    public BLEScanner(BluetoothAdapter adapter, Messenger messenger) {
        mAdapter = adapter;
        mMessenger = messenger;

        //get scanner
        mScanner = mAdapter.getBluetoothLeScanner();

        //create an handler for delayed tasks
        mHandler = new Handler();

        //setup filter
        mScanFilter = new ScanFilter.Builder().setServiceUuid(BLENetworkService.Service_UUID).build();

        //setup settings
        mSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        //create receiver
        mReceiver = new BLEReceiver(mMessenger);

    }


    public void scan(final int duration, final ActionListener listener) {
        Log.v("BLEScanner", "scan for " + String.valueOf(duration));
        if (!isActive) {
            //start
            start(new ActionListener() {

                @Override
                public void onSuccess() {
                    isActive = true;
                    Log.v("BLEScanner", "start scan");

                    //use handler for stopping after duration - 1 shot
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("BLEScanner", "scan expired");
                            stop(listener);
                        }
                    }, duration);

                }

                @Override
                public void onFailure(int error) {
                    Log.v("BLEScanner", "fail scan");
                    //call listener
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                }
            });
        }


    }

    public void start(final ActionListener listener) {
        if (!isActive) {
            Log.v("BLEScanner", "start scanner");

            List<ScanFilter> filters = new ArrayList<>();
            filters.add(mScanFilter);
            mScanner.startScan(filters, mSettings, mReceiver);


        }
        if (listener != null) {
            listener.onSuccess();
        }
    }

    public void stop(final ActionListener listener) {
        if (isActive) {
            Log.v("BLEScanner", "stop scanner");

            mScanner.stopScan(mReceiver);

            //flush
            mScanner.flushPendingScanResults(mReceiver);

            isActive = false;
        }
        if (listener != null) {
            //call listener
            listener.onSuccess();
        }
    }

}
