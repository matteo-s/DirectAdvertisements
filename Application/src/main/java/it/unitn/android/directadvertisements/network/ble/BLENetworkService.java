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
import it.unitn.android.directadvertisements.app.ServiceConnector;
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
    public static final int ADVERTISE_PAUSE = 5000;

    public static final int SCAN_DURATION = 29000;
    public static final int SCAN_PAUSE = 1000;

    public static final int SLEEP_DURATION = 1000;

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
    private BLEAdvertiser mAdvertiser;
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

    public BLENetworkService(Context context, ServiceConnector serviceConnector) {
        mAdapter = null;
        isAvailable = false;
        isSupported = false;

        this.mMessage = null;

        this.mContext = context;
        this.mService = serviceConnector;

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
        mService.unbindService();


    }

    @Override
    public void activate() {
        //bind
        mService.bindService();

        //start scanner
        if (mScanner == null) {
            mScanner = new BLEScanner(mAdapter, mService);
        }

        //start advertiser
        if (mAdvertiser == null) {
            //create
            mAdvertiser = new BLEAdvertiser(mAdapter, mService);

        }

        //send message
        mService.sendMessage(MessageKeys.NETWORK_INFO, null);

        //start indefinitely
        isActive = true;

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

        //stop loopers
        hasCycleLooper = false;
        hasAdvertiseLooper = false;
        hasDiscoveryLooper = false;

        //clear status
        hasCyclePending = false;
        hasAdvertisePending = false;
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
        Log.v("BLENetworkService", "broadcast");

        //replace message
        mMessage = BLENetworkMessage.parse(msg);


        if (!isActive) {
            //start broadcasting indefinitely
            isActive = true;
        }
//        advertiseAndDiscoveryLooper(true);
//        advertiseLooper();

        //stop if active
        if (hasAdvertisePending) {
            //stop, let active looper restart with new msg
            mAdvertiser.stop(new ActionListener() {
                @Override
                public void onSuccess() {
                    if (!hasAdvertiseLooper) {
                        advertiseLooper();
                    }
                }

                @Override
                public void onFailure(int error) {
                }
            });
        } else {
            //check for looper, maybe is sleeping
            if (!hasAdvertiseLooper) {
                advertiseLooper();
            }
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
        Log.v("BLENetworkService", "receive msg from " + String.valueOf(msg.sender));
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
    private void advertiseAndDiscoveryLooper(boolean check) {
        Log.v("BLENetworkService", "advertiseAndDiscoveryLooper hasPending " + String.valueOf(hasCyclePending) + " hasLooper " + String.valueOf(hasCycleLooper));

        if (isActive && (!hasCycleLooper || !check)) {
            hasCycleLooper = true;

            advertiseAndDiscovery(new ActionListener() {
                @Override
                public void onSuccess() {
                    //call again after random sleep
                    Random rand = new Random();
                    int delay = rand.nextInt((SLEEP_DURATION - 1) + 1) + SLEEP_DURATION;
                    Log.v("BLENetworkService", "advertiseAndDiscoveryLooper onSuccess sleep for " + String.valueOf(delay));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("BLENetworkService", "advertiseAndDiscoveryLooper onSuccess callback run");
                            advertiseAndDiscoveryLooper(false);
                        }
                    }, delay);
                }

                @Override
                public void onFailure(int error) {
                    //call again after random sleep
                    Random rand = new Random();
                    int delay = 2 * (rand.nextInt((SLEEP_DURATION - 1) + 1) + SLEEP_DURATION);
                    Log.v("BLENetworkService", "advertiseAndDiscoveryLooper onFailure error " + String.valueOf(error) + " sleep for " + String.valueOf(delay));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("BLENetworkService", "advertiseAndDiscoveryLooper onFailure callback run");
                            advertiseAndDiscoveryLooper(false);
                        }
                    }, delay);

                }
            });
        }
    }

    private void advertiseAndDiscovery(final ActionListener listener) {
        Log.v("BLENetworkService", "advertiseAndDiscovery hasPending " + String.valueOf(hasCyclePending));

        if (!hasCyclePending) {
            hasCyclePending = true;

            //start advertiser
            mAdvertiser.advertise(mMessage, ADVERTISE_DURATION, new ActionListener() {
                @Override
                public void onSuccess() {

                    Log.v("BLENetworkService", "advertiseAndDiscovery onSuccess");

                    //call scan after advertise stop
                    mScanner.scan(SCAN_DURATION, new ActionListener() {
                        @Override
                        public void onSuccess() {
                            hasCyclePending = false;

                            Log.v("BLENetworkService", "advertiseAndDiscovery discovery onSuccess");

                            //callback
                            listener.onSuccess();
                        }

                        @Override
                        public void onFailure(int error) {
                            hasCyclePending = false;
                            Log.v("BLENetworkService", "advertiseAndDiscovery discovery onFailure error " + String.valueOf(error));

                            //callback
                            listener.onFailure(error);
                        }
                    });

                }

                @Override
                public void onFailure(int error) {
                    hasCyclePending = false;
                    Log.v("BLENetworkService", "advertiseAndDiscovery onFailure error " + String.valueOf(error));

                    //callback
                    listener.onFailure(error);
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

    private void advertiseLooper() {
        Log.v("BLENetworkService", "advertiseLooper");

        if (isActive && !hasAdvertiseLooper) {
            hasAdvertiseLooper = true;

            advertise(new ActionListener() {
                @Override
                public void onSuccess() {
                    hasAdvertiseLooper = false;

                    //call again after random sleep
                    Random rand = new Random();
                    int delay = rand.nextInt((SLEEP_DURATION - 1) + 1) + ADVERTISE_PAUSE;
                    Log.v("BLENetworkService", "advertiseLooper onSuccess sleep for " + String.valueOf(delay));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("BLENetworkService", "advertiseLooper callback run");
                            advertiseLooper();
                        }
                    }, delay);
                }

                @Override
                public void onFailure(int error) {
                    hasAdvertiseLooper = false;

                    //call again after random sleep
                    Random rand = new Random();
                    int delay = 2 * (rand.nextInt((SLEEP_DURATION - 1) + 1) + ADVERTISE_PAUSE);
                    Log.v("BLENetworkService", "advertiseLooper onFailure error " + String.valueOf(error) + " sleep for " + String.valueOf(delay));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("BLENetworkService", "advertiseLooper callback run");
                            advertiseLooper();
                        }
                    }, delay);

                }
            });
        }
    }

    private void advertise(final ActionListener listener) {
        Log.v("BLENetworkService", "advertise hasPending " + String.valueOf(hasAdvertisePending));

        if (!hasAdvertisePending) {
            hasAdvertisePending = true;

            //start advertiser
            mAdvertiser.advertise(mMessage, ADVERTISE_DURATION, new ActionListener() {
                @Override
                public void onSuccess() {
                    hasAdvertisePending = false;

                    Log.v("BLENetworkService", "advertise onSuccess");
                    listener.onSuccess();
                }

                @Override
                public void onFailure(int error) {
                    hasAdvertisePending = false;
                    Log.v("BLENetworkService", "advertise onFailure error " + String.valueOf(error));

                    //callback
                    listener.onFailure(error);
                }
            });
        } else {
            listener.onFailure(0);
        }

    }


    private void discoveryLooper() {
        Log.v("BLENetworkService", "discoveryLooper");

        if (isActive && !hasDiscoveryLooper) {
            hasDiscoveryLooper = true;

            discovery(new ActionListener() {
                @Override
                public void onSuccess() {
                    hasDiscoveryLooper = false;

                    //call again after random sleep
                    Random rand = new Random();
                    int delay = rand.nextInt((SLEEP_DURATION - 1) + 1) + SCAN_PAUSE;
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
                    hasDiscoveryLooper = false;

                    //call again after random sleep
                    Random rand = new Random();
                    int delay = 2 * (rand.nextInt((SLEEP_DURATION - 1) + 1) + SCAN_PAUSE);
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
        Log.v("BLENetworkService", "discovery hasPending " + String.valueOf(hasDiscoveryPending));

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
