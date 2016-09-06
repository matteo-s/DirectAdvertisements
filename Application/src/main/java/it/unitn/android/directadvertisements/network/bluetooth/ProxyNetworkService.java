/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.util.UUID;

import it.unitn.android.directadvertisements.app.MessageKeys;
import it.unitn.android.directadvertisements.app.ServiceConnector;
import it.unitn.android.directadvertisements.log.LogService;
import it.unitn.android.directadvertisements.network.NetworkMessage;
import it.unitn.android.directadvertisements.network.NetworkNode;
import it.unitn.android.directadvertisements.network.NetworkService;
import it.unitn.android.directadvertisements.registry.NetworkRegistryUtil;

public class ProxyNetworkService implements NetworkService {

    /*
    * Constants
     */
    public static final UUID Service_UUID = UUID
            .fromString("0000b81d-0000-1000-8001-00805f9b34fb");


    /*
* Context
 */
    private Context mContext;
    private ServiceConnector mService = null;
    private LogService mLogger = null;
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
    boolean hasConnection = false;


    public ProxyNetworkService(Context context, ServiceConnector serviceConnector, LogService logger) {
        mAdapter = null;
        isAvailable = false;
        isSupported = false;
        isConnected = false;

        this.mMessage = null;
        this.mDevice = null;

        this.mContext = context;
        this.mService = serviceConnector;
        this.mLogger = logger;

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
            mAdapter = BluetoothAdapter.getDefaultAdapter();

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
            if (mClient == null) {
                mClient = new ProxyClient(mAdapter, mService, mLogger);
            }

            //request connection
            mClient.connect(mDevice);

            if (mClient.isConnected()) {
                //start indefinitely
                isActive = true;

                mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

            } else {
                deactivate();
            }

        }

    }

    @Override
    public void deactivate() {
        Log.v("ProxyNetworkService", "deactivate");

        //set inactive
        isActive = false;

        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (Exception ex) {
        }

        if (mClient != null) {
            mClient.disconnect();
        }


        //clear status
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
        //replace message
        mMessage = ProxyNetworkMessage.parse(msg);

        if (mClient != null && mClient.isConnected()) {
            mClient.send(mMessage);
        }
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

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                    BluetoothDevice.ERROR);

            if (state == BluetoothDevice.BOND_BONDED) {
                Log.v("ProxyNetworkService", "Device " + device + " PAIRED");
            } else if (state == BluetoothDevice.BOND_BONDING) {
                Log.v("ProxyNetworkService", "Device " + device + " pairing is in process...");
            } else if (state == BluetoothDevice.BOND_NONE) {
                Log.v("ProxyNetworkService", "Device " + device + " is unpaired");

                //check for active connection and restart
                if (isActive) {
                    if (mClient != null && mDevice != null) {
                        mClient.disconnect();
                        mClient.connect(mDevice);
                    }
                }


            } else {
                Log.v("ProxyNetworkService", "Device " + device + " is in undefined state");
            }
        }
    };

}
