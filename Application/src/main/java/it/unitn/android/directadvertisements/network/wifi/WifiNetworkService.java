/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.wifi;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import it.unitn.android.directadvertisements.app.MessageKeys;
import it.unitn.android.directadvertisements.network.NetworkMessage;
import it.unitn.android.directadvertisements.network.NetworkNode;
import it.unitn.android.directadvertisements.network.NetworkService;
import it.unitn.android.directadvertisements.registry.NetworkRegistryUtil;

public class WifiNetworkService implements NetworkService {

//public class WifiNetworkService extends Service implements NetworkService {
//    /*
//    * Service
//     */
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.v("WifiNetworkService", "onStartCommand");
//        return START_STICKY;
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }


    /*
    * Constants
     */
    public static final ParcelUuid Service_UUID = ParcelUuid
            .fromString("0000b81d-0000-1000-8000-00805f9b34fb");

    public static final String SERVICE_INSTANCE = "_directadvertisement";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";
    public static final int SERVICE_PORT = 5001;

    public static final int ADVERTISE_DURATION = 3000;
    public static final int DISCOVERY_DURATION = 9000;
    public static final int SLEEP_DURATION = 1000;
    public static final int PEERS_DURATION = 30000;

    public static final boolean MODE_ACTIVE = false;

    /*
    * Context
     */
    private Context mContext;


    /*
    * WIFI
     */
    private WifiP2pManager mManager;
    private Channel mChannel;
    private Messenger mMessenger;
    private Handler mHandler;

    boolean isBound = false;
    boolean isActive = false;
    boolean hasPending = false;
    boolean hasConnection = false;

    private WifiReceiver mReceiver;
    private WifiAdvertiser mAdvertiser;
    private WifiDiscovery mDiscovery;

    private NetworkNode mNode;
    private WifiServer mServer;
    private WifiClient mClient;

    private WifiNetworkMessage mMessage;

    public WifiNetworkService(Context context, IBinder binder) {
        this.mManager = null;
        this.mChannel = null;
        this.mReceiver = null;
        this.mAdvertiser = null;
        this.mDiscovery = null;

        this.mNode = null;

        this.mMessage = null;

        this.mContext = context;
        this.mMessenger = new Messenger(binder);
        this.isBound = true;

        //create an handler for delayed tasks
        mHandler = new Handler();
    }

    public void setup(WifiP2pManager manager, Channel channel) {
        this.mManager = manager;
        this.mChannel = channel;
    }

    @Override
    public String getNetwork() {
        return NetworkService.SERVICE_WIFI;
    }

    @Override
    public boolean isSupported() {
        //check for wifi support - TODO
        return true;
    }

    @Override
    public boolean isAvailable() {
        //check for wifi status
        return true;
    }

    @Override
    public boolean isConfigured() {
        //check for services
        return (this.mManager != null && this.mChannel != null);
    }

    @Override
    public void init() {
        if (isConfigured()) {
            //nothing to do, callback will populate device
        }
    }

    @Override
    public void destroy() {
        // unbind or process might have crashes
        mMessenger = null;
        isBound = false;
    }

    @Override
    public void activate() {
        if (mReceiver == null) {
            //create
            if (MODE_ACTIVE) {

                mReceiver = new WifiReceiver(mManager, mChannel, mMessenger, MODE_ACTIVE, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.v("WifiNetworkService", "listener connected");

                        //active connection

                        //DO inquiry on socket


                        //disconnect
                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                //wifiReceiver will notify event
                                Log.v("WifiNetworkService", "group remove requested");

                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.v("WifiNetworkService", "group remove fail");
                                hasConnection = false;
                            }
                        });


                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.v("WifiNetworkService", "listener disconnect");

                        //disconnected
                        hasConnection = false;
                    }
                });
            } else {
                mReceiver = new WifiReceiver(mManager, mChannel, mMessenger);
            }
        }

        //register intent
        IntentFilter intentFilter = new IntentFilter();
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        //register receiver
        mContext.registerReceiver(mReceiver, intentFilter);

        //start advertiser
        if (mAdvertiser == null) {
            //create
            mAdvertiser = new WifiAdvertiser(mManager, mChannel, mMessenger);

        }

        if (mDiscovery == null) {
            //create
            mDiscovery = new WifiDiscovery(mManager, mChannel, mMessenger);

        }


        //check mode
        if (MODE_ACTIVE) {
            //start only discovery
            isActive = true;
            discoveryLooper();

        } else {
            //start discovery
            discovery(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(int error) {
                }
            });
//        mDiscovery.discovery();
//        mDiscovery.startDelay(2*ADVERTISE_DURATION);
        }

        //initial discovery
//        if (!isActive && !hasPending) {
//            hasPending = true;
//
//            mDiscovery.discovery(DISCOVERY_DURATION, new WifiP2pManager.ActionListener() {
//                @Override
//                public void onSuccess() {
//                    hasPending = false;
//                }
//
//                @Override
//                public void onFailure(int error) {
//                    hasPending = false;
//                }
//            });
//        }


        sendMessage(MessageKeys.NETWORK_START, null);
    }

    @Override
    public void deactivate() {
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
        }
        if (mAdvertiser != null) {
            //stop
            mAdvertiser.stop(null);
        }
        if (mDiscovery != null) {
            //stop
            mDiscovery.stop(null);
        }

        //set inactive
        isActive = false;

        //clear status
        hasPending = false;
        hasConnection = false;

        sendMessage(MessageKeys.NETWORK_STOP, null);
    }


    @Override
    public boolean isActive() {
        return isActive;
    }

    /*
    * Unicast - not supported
     */

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
        Log.v("WifiNetworkService", "broadcast");

        //replace message
        mMessage = WifiNetworkMessage.parse(msg);


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
    public void multicast(String[] addresses, NetworkMessage msg) {
        throw new UnsupportedOperationException();
    }

    public void multicast(int[] ids, NetworkMessage msg) {
        throw new UnsupportedOperationException();

    }

/*
* inquiry
 */

    @Override
    public void inquiry(final String address) {
        Log.v("WifiNetworkService", "inquiry " + address);

        if (!hasConnection) {
            //fetch peer
            WifiP2pDevice peer = null;
            Iterator<WifiP2pDevice> iter = mReceiver.mPeers.iterator();
            while (iter.hasNext()) {
                WifiP2pDevice p = iter.next();
                if (p.deviceAddress.equals(address)) {
                    peer = p;
                    break;
                }

            }

            if (peer != null) {
                hasConnection = true;
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = peer.deviceAddress;
                config.wps.setup = WpsInfo.PBC;

                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        //wifiReceiver will notify event
                        Log.v("WifiNetworkService", "connect requested for " + address);

                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.v("WifiNetworkService", "connect failed for  " + address);
                        hasConnection = false;
                    }
                });


            }

        } else {
            Log.v("WifiNetworkService", "inquiry pending");
        }

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


    @Override
    public NetworkNode info() {
        NetworkNode node = null;

        if (isConfigured() && mReceiver != null) {
            WifiP2pDevice device = mReceiver.getDevice();
            if (device != null) {
                //use placeholder to return values
                node = new NetworkNode();
                node.address = device.deviceAddress;
                node.name = device.deviceName;
            }

        }

        return node;
    }

    /*
    * Helpers
     */
    private void advertiseAndDiscoveryLooper() {
        Log.v("WifiNetworkService", "advertiseAndDiscoveryLooper");

        if (isActive) {
            advertiseAndDiscovery(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    //call again after random sleep
                    Random rand = new Random();
                    int delay = rand.nextInt((SLEEP_DURATION - 500) + 1) + 500;
                    Log.v("WifiNetworkService", "advertiseAndDiscoveryLooper sleep for " + String.valueOf(delay));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("WifiNetworkService", "advertiseAndDiscoveryLooper callback run");
                            advertiseAndDiscoveryLooper();
                        }
                    }, delay);
                }

                @Override
                public void onFailure(int error) {
                    //call again after random sleep
                    Random rand = new Random();
                    int delay = rand.nextInt((2 * SLEEP_DURATION - 500) + 1) + 500;
                    Log.v("WifiNetworkService", "advertiseAndDiscoveryLooper sleep for " + String.valueOf(delay));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("WifiNetworkService", "advertiseAndDiscoveryLooper callback run");
                            advertiseAndDiscoveryLooper();
                        }
                    }, delay);

                }
            });
        }
    }


    private void advertiseAndDiscovery(final WifiP2pManager.ActionListener listener) {
        Log.v("WifiNetworkService", "advertiseAndDiscovery");
        if (!hasPending) {
            hasPending = true;

            //start advertiser
            mAdvertiser.advertise(mMessage, ADVERTISE_DURATION, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {


                    //call discovery after advertise stop
                    mDiscovery.discovery(DISCOVERY_DURATION, new WifiP2pManager.ActionListener() {
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

                }

                @Override
                public void onFailure(int error) {
                    hasPending = false;
                }
            });
        } else {
            listener.onFailure(0);
        }

//        //call discovery after advertise stop
//        mDiscovery.discovery(DISCOVERY_DURATION, new WifiP2pManager.ActionListener() {
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
        Log.v("WifiNetworkService", "discoveryLooper");

        if (isActive) {
            discovery(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    //call again after random sleep
                    Random rand = new Random();
                    int delay = rand.nextInt((SLEEP_DURATION - 500) + 1) + 500;
                    Log.v("WifiNetworkService", "discoveryLooper sleep for " + String.valueOf(delay));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("WifiNetworkService", "discoveryLooper callback run");
                            discoveryLooper();
                        }
                    }, delay);
                }

                @Override
                public void onFailure(int error) {
                    //call again after random sleep
                    Random rand = new Random();
                    int delay = rand.nextInt((2 * SLEEP_DURATION - 500) + 1) + 500;
                    Log.v("WifiNetworkService", "discoveryLooper sleep for " + String.valueOf(delay));

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("WifiNetworkService", "discoveryLooper callback run");
                            discoveryLooper();
                        }
                    }, delay);

                }
            });
        }
    }


    private void discovery(final WifiP2pManager.ActionListener listener) {
        if (!hasPending) {
            hasPending = true;


            mDiscovery.discoverPeers(PEERS_DURATION, new WifiP2pManager.ActionListener() {
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
