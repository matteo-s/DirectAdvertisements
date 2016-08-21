/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.proxy;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
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
import it.unitn.android.directadvertisements.app.ServiceConnector;
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
    private ServiceConnector mService = null;
    private Handler mHandler;

    /*
    * Bluetooth
     */
    private BluetoothAdapter mAdapter;
    private String mDevice;


    private ProxyNetworkMessage mMessage;
    private ProxyClient mClient;

    private boolean isAvailable;
    private boolean isSupported;

     /*
    * Data
     */


    boolean isConnected = false;
    boolean isActive = false;
    boolean hasPending = false;
    boolean hasConnection = false;
    boolean hasLooper = false;


    public ProxyNetworkService(Context context, ServiceConnector serviceConnector) {
        mAdapter = null;
        isAvailable = false;
        isSupported = false;
        isConnected = false;

        this.mMessage = null;
        this.mDevice = null;

        this.mContext = context;
        this.mService = serviceConnector;

        //create an handler for delayed tasks
        mHandler = new Handler();
    }

    @Override
    public String getNetwork() {
        return NetworkService.SERVICE_PROXY;
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
    public void init(Bundle bundle) {
        Log.v("ProxyNetworkService", "init");

        //check for device
        if (bundle.containsKey("network.proxy")) {
            this.mDevice = bundle.getString("network.proxy");

            Log.v("ProxyNetworkService", "init for device " + mDevice);
            mAdapter = ((BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE))
                    .getAdapter();

            if (mAdapter != null) {
                isSupported = true;

                //check if active
                if (mAdapter.isEnabled()) {
                    isAvailable = true;

                } else {
                    //set not active
                    isAvailable = false;
                }
            }

        }

    }


    @Override
    public void destroy() {
        deactivate();

        // unbind or process might have crashes
        mService.unbindService();
    }

    @Override
    public void activate() {
        Log.v("ProxyNetworkService", "activate");

        //bind
        mService.bindService();

        if (mDevice == null) {
            //no proxy device, stop
            deactivate();

        } else {
            //start connection


            //start indefinitely
            isActive = true;
        }

    }

    @Override
    public void deactivate() {
        Log.v("ProxyNetworkService", "deactivate");

        //set inactive
        isActive = false;

        //stop looper
        hasLooper = false;

        //clear status
        hasPending = false;
        hasConnection = false;

        mService.sendMessage(MessageKeys.NETWORK_STOP, null);
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


}
