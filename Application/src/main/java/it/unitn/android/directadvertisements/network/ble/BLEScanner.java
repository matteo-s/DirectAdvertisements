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

import it.unitn.android.directadvertisements.app.ServiceConnector;
import it.unitn.android.directadvertisements.log.LogService;

public class BLEScanner {


    private BluetoothAdapter mAdapter;
    private BluetoothLeScanner mScanner;
    private ScanFilter mScanFilter;
    private ScanSettings mSettings;

    private BLEReceiver mReceiver;


    private boolean isActive = false;
    private Handler mHandler;


    public BLEScanner(BluetoothAdapter adapter, ServiceConnector serviceConnector, LogService logger) {
        mAdapter = adapter;

        //get scanner
        mScanner = mAdapter.getBluetoothLeScanner();

        //create an handler for delayed tasks
        mHandler = new Handler();

        //setup filter for service data
//        mScanFilter = new ScanFilter.Builder().setServiceUuid(BLENetworkService.Service_UUID).build();
        //setup filter for manufacturer data
        mScanFilter = buildScanFilter();

        //setup settings
//        mSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
        mSettings = buildScanSettings();

        //dump
        Log.v("BLEScanner", "scanSettings" + mSettings.toString());

        //create receiver
        mReceiver = new BLEReceiver(serviceConnector, logger);

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

    public ScanSettings buildScanSettings() {
        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
        settingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        //enforce immediate report from ble stack to app
        settingsBuilder.setReportDelay(0);
        //can't set all matches, so depends on device if packets from same source are cached/filtered
//        settingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        return settingsBuilder.build();
    }

    public ScanFilter buildScanFilter() {
        ScanFilter.Builder mBuilder = new ScanFilter.Builder();
        //use template msg
        BLENetworkMessage msg = new BLENetworkMessage();
        byte[] template = msg.buildTemplateData();
        //scan mask - only uuid checked: first 2 bytes
        byte[] mask = new byte[template.length];
        //check flag
        mask[0] = (byte) 0x01;
        mask[1] = (byte) 0x01;
        for (int i = 2; i < mask.length; i++) {
            //ignore flag
            mask[i] = (byte) 0x00;
        }

        mBuilder.setManufacturerData(224, template, mask);
        return mBuilder.build();
    }

}
