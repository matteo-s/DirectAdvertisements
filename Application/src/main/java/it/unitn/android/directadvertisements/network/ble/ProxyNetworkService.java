/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Random;

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
    public static final ParcelUuid Service_UUID = ParcelUuid
            .fromString("0000b81d-0000-1000-8000-00805f9b34fb");

//    public static final int ADVERTISE_DURATION = 5000;
//    public static final int ADVERTISE_PAUSE = 5000;

    public static final int SCAN_DURATION = 29000;
    public static final int SCAN_PAUSE = 1000;

    public static final int SLEEP_DURATION = 1000;

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
    private ProxyClient mProxy;
    private BLEScanner mScanner;

    private BLENetworkMessage mMessage;

    private boolean isAvailable;
    private boolean isSupported;


    /*
    * Data
     */


    boolean isActive = false;
    boolean hasConnection = false;

//    boolean hasPending = false;
//    boolean hasLooper = false;

    boolean hasCyclePending = false;
    boolean hasCycleLooper = false;

    boolean hasAdvertisePending = false;
    boolean hasAdvertiseLooper = false;

    boolean hasDiscoveryPending = false;
    boolean hasDiscoveryLooper = false;

    public ProxyNetworkService(Context context, ServiceConnector serviceConnector, LogService logger) {
        mAdapter = null;
        isAvailable = false;
        isSupported = false;

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
    public void init(Bundle bundle) {


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
//                //read address
//                _address = readAddress(mAdapter);

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
        //bind
        mService.bindService();

        //start scanner
        if (mScanner == null) {
            mScanner = new BLEScanner(mAdapter, mService, mLogger);
        }

        //start proxy
        if (mProxy == null) {
            //create
            mProxy = new ProxyClient(mAdapter, mService, mContext, mLogger);

        }

        //request connection
        mProxy.connect(mDevice);

        //send message
        mService.sendMessage(MessageKeys.NETWORK_INFO, null);

        //start indefinitely
        isActive = true;

        //logger
        mLogger.info("ble", "activate");


//        //start one-shot discovery
//        discovery(new ActionListener() {
//            @Override
//            public void onSuccess() {
//            }
//
//            @Override
//            public void onFailure(int error) {
//            }
//        });


        //start discovery looper
        discoveryLooper();
    }

    @Override
    public void deactivate() {
        if (mProxy != null) {
            mProxy.disconnect();
        }

        if (mScanner != null) {
            //stop
            mScanner.stop(null);
        }

        //set inactive
        isActive = false;

        //logger
        mLogger.info("ble", "deactivate");

        //stop loopers
        hasDiscoveryLooper = false;

        //clear status
        hasDiscoveryPending = false;


        hasConnection = false;

        mService.sendMessage(MessageKeys.NETWORK_STOP, null);

        // unbind
        mService.unbindService();
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
        Log.v("ProxyNetworkService", "broadcast");

        //replace message
        mMessage = BLENetworkMessage.parse(msg);


        if (!isActive) {
            //start broadcasting indefinitely
            isActive = true;
        }


        if (mProxy != null && mProxy.isConnected()) {
            mProxy.send(mMessage);
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
    * Network
     */


    private void discoveryLooper() {
        Log.v("ProxyNetworkService", "discoveryLooper");

        if (isActive && !hasDiscoveryLooper) {
            hasDiscoveryLooper = true;

            discovery(new ActionListener() {
                @Override
                public void onSuccess() {
                    hasDiscoveryLooper = false;

                    //call again after random sleep
                    Random rand = new Random();
                    int delay = rand.nextInt((SLEEP_DURATION - 1) + 1) + SCAN_PAUSE;
                    Log.v("ProxyNetworkService", "discoveryLooper onSuccess sleep for " + String.valueOf(delay));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("ProxyNetworkService", "discoveryLooper callback run");
                            discoveryLooper();
                        }
                    }, delay);
                }

                @Override
                public void onFailure(int error) {
                    hasDiscoveryLooper = false;

                    //call again after random sleep
                    Random rand = new Random();
                    int delay = 2 * (rand.nextInt((SLEEP_DURATION - 1) + 1) + SCAN_PAUSE);
                    Log.v("ProxyNetworkService", "discoveryLooper onFailure error " + String.valueOf(error) + " sleep for " + String.valueOf(delay));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("ProxyNetworkService", "discoveryLooper callback run");
                            discoveryLooper();
                        }
                    }, delay);

                }
            });
        }
    }

    private void discovery(final ActionListener listener) {
        Log.v("ProxyNetworkService", "discovery hasPending " + String.valueOf(hasDiscoveryPending));

        if (!hasDiscoveryPending) {
            hasDiscoveryPending = true;

            mScanner.scan(SCAN_DURATION, new ActionListener() {
                @Override
                public void onSuccess() {
                    hasDiscoveryPending = false;

                    //callback
                    listener.onSuccess();
                }

                @Override
                public void onFailure(int error) {
                    hasDiscoveryPending = false;
                    listener.onFailure(error);
                }
            });

        } else {
            //callback
            listener.onFailure(0);
        }
    }

    /*
    * Helpers - ble
     */

    protected String readAddress(BluetoothAdapter adapter) {
        return adapter.getAddress();
    }

}
