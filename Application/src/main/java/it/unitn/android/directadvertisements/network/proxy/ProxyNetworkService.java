/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.proxy;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import it.unitn.android.directadvertisements.app.MessageKeys;
import it.unitn.android.directadvertisements.network.NetworkMessage;
import it.unitn.android.directadvertisements.network.NetworkNode;
import it.unitn.android.directadvertisements.network.NetworkService;
import it.unitn.android.directadvertisements.network.ble.BLEAdvertiser;
import it.unitn.android.directadvertisements.network.ble.BLENetworkMessage;
import it.unitn.android.directadvertisements.network.ble.BLEScanner;
import it.unitn.android.directadvertisements.registry.NetworkRegistryUtil;

public class ProxyNetworkService implements NetworkService {

    /*
    * Constants
     */
    public static final ParcelUuid Service_UUID = ParcelUuid
            .fromString("0000b81d-0000-1000-8000-00805f9b34fb");


    /*
* Context
 */
    private Context mContext;
    private Messenger mMessenger;
    private Handler mHandler;

    /*
    * Bluetooth
     */
    private BluetoothAdapter mAdapter;


    private ProxyNetworkMessage mMessage;

    private boolean isAvailable;
    private boolean isSupported;

     /*
    * Data
     */


    boolean isBound = false;
    boolean isActive = false;
    boolean hasPending = false;
    boolean hasConnection = false;
    boolean hasLooper = false;


    public ProxyNetworkService(Context context, IBinder binder) {
        mAdapter = null;
        isAvailable = false;
        isSupported = false;

        this.mMessage = null;

        this.mContext = context;
        this.mMessenger = new Messenger(binder);
        this.isBound = true;

        //create an handler for delayed tasks
        mHandler = new Handler();
    }

    @Override
    public String getNetwork() {
        return NetworkService.SERVICE_BLE;
    }

    @Override
    public boolean isSupported() {
        return isSupported;
    }

    @Override
    public boolean isAvailable() {
        return isAvailable;
    }

    public boolean isConfigured() {
        return (mAdapter != null);
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void init() {

    }


    @Override
    public void destroy() {
        deactivate();

        // unbind or process might have crashes
        mMessenger = null;
        isBound = false;


    }

    @Override
    public void activate() {

    }

    @Override
    public void deactivate() {

        //set inactive
        isActive = false;

        //stop looper
        hasLooper = false;

        //clear status
        hasPending = false;
        hasConnection = false;

        sendMessage(MessageKeys.NETWORK_STOP, null);
    }


    @Override
    public void send(String address, NetworkMessage msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(int id, NetworkMessage msg) {
        throw new UnsupportedOperationException();
    }

    /*
    * Broadcast
     */
    @Override
    public void broadcast(NetworkMessage msg) {

    }


    /*
  * Multicast - not supported
   */
    @Override
    public void multicast(String[] addresses, NetworkMessage msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void multicast(int[] ids, NetworkMessage msg) {
        throw new UnsupportedOperationException();
    }

    /*
    * inquiry
     */
    @Override
    public void inquiry(String address) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void inquiry(int id) {
        NetworkNode node = NetworkRegistryUtil.getRegistry().getNode(id);
        if (node != null) {
            inquiry(node.address);
        }
    }


    /*
    * Receive
     */
    @Override
    public void receive(NetworkMessage msg) {
        //ignore
        Log.v("ProxyNetworkService", "receive msg from " + String.valueOf(msg.sender));
    }
/*
*
 */

    @Override
    public void getIdentifier() {

    }

    public NetworkNode info() {
        NetworkNode node = null;

        if (isConfigured() && mAdapter != null) {
            //use placeholder to return values
            node = new NetworkNode();
            node.address = mAdapter.getAddress();
            node.name = mAdapter.getName();

        }

        return node;
    }

     /*
    * Helpers - ble
     */

    protected String readAddress(BluetoothAdapter adapter) {
        return adapter.getAddress();
    }


     /*
    * Helper - message
     */

    protected void sendMessage(int key, Bundle bundle) {
        if (isBound) {
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
