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

public class BLEReceiver extends ScanCallback {

    private Messenger mMessenger;

    public BLEReceiver(Messenger messenger) {
        mMessenger = messenger;
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        //call super first
        super.onScanResult(callbackType, result);

        //check data
        if (result != null
                && result.getDevice() != null
                && result.getScanRecord() != null) {

            ScanRecord record = result.getScanRecord();
            String address = result.getDevice().getAddress();
            String name = result.getDevice().getName();

            if (record.getServiceData().containsKey(BLENetworkService.Service_UUID)) {
                byte[] bytes = record.getServiceData(BLENetworkService.Service_UUID);

                Log.v("BLEReceiver", "receive data from " + address + " data length " + String.valueOf(bytes.length) + " : " + BLENetworkMessage.byteArrayToString(bytes));

                //get message from bytes
                BLENetworkMessage n = BLENetworkMessage.parse(bytes);

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


                //directly send to service
                Message msg = Message.obtain(null, MessageKeys.CLOCK_RECEIVE, 0, 0);
                Bundle bundle = new Bundle();
                bundle.putSerializable("n", n);

                msg.setData(bundle);
                try {
                    mMessenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }


        }

    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        super.onBatchScanResults(results);
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
    }


    protected void sendMessage(int key, Bundle bundle) {
        if (mMessenger != null) {
            Message msg = Message.obtain(null, key, 0, 0);

            // Set the bundle data to the Message
            if (bundle != null) {
                msg.setData(bundle);
            }
            // Send the Message to the Service (in another process)
            try {
                mMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
