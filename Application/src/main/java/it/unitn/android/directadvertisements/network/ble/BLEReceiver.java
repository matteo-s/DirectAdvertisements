/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.ble;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

import it.unitn.android.directadvertisements.app.MessageKeys;
import it.unitn.android.directadvertisements.app.ServiceConnector;
import it.unitn.android.directadvertisements.log.LogService;
import it.unitn.android.directadvertisements.log.LogServiceUtil;

public class BLEReceiver extends ScanCallback {

    private ServiceConnector mService = null;
    private LogService mLogger;

    public BLEReceiver(ServiceConnector serviceConnector) {
        this.mService = serviceConnector;
        mLogger = LogServiceUtil.getLogger();
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
//        //call super first - DISABLED
//        super.onScanResult(callbackType, result);

        //check data
        if (result != null
                && result.getDevice() != null
                && result.getScanRecord() != null) {

            ScanRecord record = result.getScanRecord();
            String address = result.getDevice().getAddress();
            int rssi = result.getRssi();
            //name not sent
//            String name = result.getDevice().getName();

            Log.v("BLEReceiver", "receive result from " + address + " rssi " + String.valueOf(rssi));


            //check for service data
            if (record.getServiceData().containsKey(BLENetworkService.Service_UUID)) {
                byte[] bytes = record.getServiceData(BLENetworkService.Service_UUID);

                Log.v("BLEReceiver", "receive data from " + address + " data length " + String.valueOf(bytes.length) + " : " + BLENetworkMessage.byteArrayToString(bytes));


                //get message from service data bytes
                BLENetworkMessage n = BLENetworkMessage.parseServiceData(bytes);

                //set sender address
                n.address = address;

                StringBuilder vector = new StringBuilder();
                for (int i = 1; i <= BLENetworkMessage.SLOTS; i++) {
                    if (n.clocks.containsKey(i)) {
                        vector.append(Short.toString(n.clocks.get(i)));
                    } else {
                        vector.append("0");
                    }
                }

                Log.v("BLEReceiver", "received msg from " + address + " : " + vector.toString());

                //log to file
                mLogger.info("ble", "received msg from " + address + " : " + vector.toString());

                //directly send to service
                Bundle bundle = new Bundle();
                bundle.putSerializable("n", n);

                mService.sendMessage(MessageKeys.CLOCK_RECEIVE, bundle);
            }

            //check for manufacturer data
            byte[] manufacturerData = record.getManufacturerSpecificData(224);
            if (manufacturerData != null) {
                byte[] bytes = manufacturerData;

                Log.v("BLEReceiver", "receive data from " + address + " data length " + String.valueOf(bytes.length) + " : " + BLENetworkMessage.byteArrayToString(bytes));


                //get message from service data bytes
                BLENetworkMessage n = BLENetworkMessage.parseManufacturerData(bytes);

                //set sender address
                n.address = address;

                StringBuilder vector = new StringBuilder();
                for (int i = 1; i <= BLENetworkMessage.SLOTS; i++) {
                    if (n.clocks.containsKey(i)) {
                        vector.append(Short.toString(n.clocks.get(i)));
                    } else {
                        vector.append("0");
                    }
                }

                Log.v("BLEReceiver", "received msg from " + address + " : " + vector.toString());

                //log to file
                mLogger.info("ble", "received msg from " + address + " : " + vector.toString());

                //directly send to service
                Bundle bundle = new Bundle();
                bundle.putSerializable("n", n);

                mService.sendMessage(MessageKeys.CLOCK_RECEIVE, bundle);

            }


        }

    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        super.onBatchScanResults(results);
    }

    @Override
    public void onScanFailed(int errorCode) {

//        super.onScanFailed(errorCode);
        Log.v("BLEReceiver", "onScanFailed " + String.valueOf(errorCode));


    }


}
