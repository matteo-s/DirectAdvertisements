/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import java.util.Random;

import it.unitn.android.directadvertisements.app.MessageKeys;
import it.unitn.android.directadvertisements.network.NetworkMessage;
import it.unitn.android.directadvertisements.network.NetworkNode;
import it.unitn.android.directadvertisements.network.NetworkService;
import it.unitn.android.directadvertisements.registry.NetworkRegistryUtil;

public class BLENetworkService implements NetworkService {

    /*
    * Constants
     */
    public static final ParcelUuid Service_UUID = ParcelUuid
            .fromString("0000b81d-0000-1000-8000-00805f9b34fb");

    public static final int ADVERTISE_DURATION = 5000;
    public static final int SCAN_DURATION = 3000;
    public static final int SLEEP_DURATION = 1000;

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
    private BLEAdvertiser mAdvertiser;
    private BLEScanner mScanner;

    private BLENetworkMessage mMessage;

    private boolean isAvailable;
    private boolean isSupported;


    /*
    * Data
     */


    boolean isBound = false;
    boolean isActive = false;
    boolean hasPending = false;
    boolean hasConnection = false;


    public BLENetworkService(Context context, IBinder binder) {
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
        mAdapter = ((BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter();

        if (mAdapter != null) {
            isSupported = true;

            //check if active
            if (mAdapter.isEnabled()) {
//                //read address
//                _address = readAddress(mAdapter);

                //check for capabilities
                if (mAdapter.isMultipleAdvertisementSupported()) {
                    //set as active
                    isAvailable = true;

                } else {
                    //disable support
                    isSupported = false;
                }

            } else {
                //set not active
                isAvailable = false;
            }
        }
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

        //start scanner
        if (mScanner == null) {
            mScanner = new BLEScanner(mAdapter, mMessenger);
        }

        //start advertiser
        if (mAdvertiser == null) {
            //create
            mAdvertiser = new BLEAdvertiser(mAdapter, mMessenger);

        }

        //send message
        sendMessage(MessageKeys.NETWORK_INFO, null);

        //start one-shot discovery
        discovery(new ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int error) {
            }
        });
    }

    @Override
    public void deactivate() {
        if (mAdvertiser != null) {
            //stop
            mAdvertiser.stop(null);
        }
        if (mScanner != null) {
            //stop
            mScanner.stop(null);
        }

        //set inactive
        isActive = false;

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
        Log.v("BLENetworkService", "broadcast");

        //replace message
        mMessage = BLENetworkMessage.parse(msg);


        if (!isActive) {
            //start broadcasting indefinitely
            isActive = true;

            advertiseAndDiscoveryLooper();

        } else {
            //drop msg
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
    private void advertiseAndDiscoveryLooper() {
        Log.v("BLENetworkService", "advertiseAndDiscoveryLooper hasPending " + String.valueOf(hasPending));

        if (isActive) {
            advertiseAndDiscovery(new ActionListener() {
                @Override
                public void onSuccess() {
                    //call again after random sleep
                    Random rand = new Random();
                    int delay = rand.nextInt((SLEEP_DURATION - SLEEP_DURATION) + 1) + SLEEP_DURATION;
                    Log.v("BLENetworkService", "advertiseAndDiscoveryLooper onSuccess sleep for " + String.valueOf(delay));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("BLENetworkService", "advertiseAndDiscoveryLooper callback run");
                            advertiseAndDiscoveryLooper();
                        }
                    }, delay);
                }

                @Override
                public void onFailure(int error) {
                    //call again after random sleep
                    Random rand = new Random();
                    int delay = rand.nextInt((2 * SLEEP_DURATION - SLEEP_DURATION) + 1) + SLEEP_DURATION;
                    Log.v("BLENetworkService", "advertiseAndDiscoveryLooper onFailure error " + String.valueOf(error) + " sleep for " + String.valueOf(delay));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("BLENetworkService", "advertiseAndDiscoveryLooper callback run");
                            advertiseAndDiscoveryLooper();
                        }
                    }, delay);

                }
            });
        }
    }

    private void advertiseAndDiscovery(final ActionListener listener) {
        Log.v("BLENetworkService", "advertiseAndDiscovery hasPending " + String.valueOf(hasPending));
        if (!hasPending) {
            hasPending = true;

            //start advertiser
            mAdvertiser.advertise(mMessage, ADVERTISE_DURATION, new ActionListener() {
                @Override
                public void onSuccess() {

                    Log.v("BLENetworkService", "advertiseAndDiscovery onSuccess");

                    //call scan after advertise stop
                    mScanner.scan(SCAN_DURATION, new ActionListener() {
                        @Override
                        public void onSuccess() {
                            hasPending = false;

                            Log.v("BLENetworkService", "advertiseAndDiscovery discovery onSuccess");

                            //callback
                            listener.onSuccess();
                        }

                        @Override
                        public void onFailure(int error) {
                            hasPending = false;
                            Log.v("BLENetworkService", "advertiseAndDiscovery discovery onFailure error " + String.valueOf(error));
                        }
                    });

                }

                @Override
                public void onFailure(int error) {
                    hasPending = false;
                    Log.v("BLENetworkService", "advertiseAndDiscovery onFailure error " + String.valueOf(error));
                }
            });
        } else {
            listener.onFailure(0);
        }

//        //call discovery after advertise stop
//        mDiscovery.discovery(DISCOVERY_DURATION, new ActionListener() {
//            @Override
//            public void onSuccess() {
//                hasPending = false;
//
//                //callback
//                listener.onSuccess();
//            }
//
//            @Override
//            public void onFailure(int error) {
//                hasPending = false;
//            }
//        });
    }


    private void discoveryLooper() {
        Log.v("BLENetworkService", "discoveryLooper");

        if (isActive) {
            discovery(new ActionListener() {
                @Override
                public void onSuccess() {
                    //call again after random sleep
                    Random rand = new Random();
                    int delay = rand.nextInt((SLEEP_DURATION - SLEEP_DURATION) + 1) + SLEEP_DURATION;
                    Log.v("BLENetworkService", "discoveryLooper onSuccess sleep for " + String.valueOf(delay));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("BLENetworkService", "discoveryLooper callback run");
                            discoveryLooper();
                        }
                    }, delay);
                }

                @Override
                public void onFailure(int error) {
                    //call again after random sleep
                    Random rand = new Random();
                    int delay = rand.nextInt((2 * SLEEP_DURATION - SLEEP_DURATION) + 1) + SLEEP_DURATION;
                    Log.v("BLENetworkService", "discoveryLooper onFailure error " + String.valueOf(error) + " sleep for " + String.valueOf(delay));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("BLENetworkService", "discoveryLooper callback run");
                            discoveryLooper();
                        }
                    }, delay);

                }
            });
        }
    }

    private void discovery(final ActionListener listener) {
        if (!hasPending) {
            hasPending = true;

            mScanner.scan(SCAN_DURATION, new ActionListener() {
                @Override
                public void onSuccess() {
                    hasPending = false;

                    //callback
                    listener.onSuccess();
                }

                @Override
                public void onFailure(int error) {
                    hasPending = false;
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
