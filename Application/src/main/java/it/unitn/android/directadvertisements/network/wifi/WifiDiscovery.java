/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.wifi;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import it.unitn.android.directadvertisements.app.MessageKeys;
import it.unitn.android.directadvertisements.network.NetworkMessage;

public class WifiDiscovery {
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private Messenger mMessenger;

    private boolean isActive = false;
    private Handler mHandler;
    private DnsSdServiceResponseListener mResponseListener;
    private DnsSdTxtRecordListener mTxtListener;
    private WifiP2pDnsSdServiceRequest mServiceRequest;

    public WifiDiscovery(WifiP2pManager manager, WifiP2pManager.Channel channel, Messenger messenger) {
        mManager = manager;
        mChannel = channel;
        mMessenger = messenger;
        mResponseListener = new DnsSdServiceResponseListener() {

            @Override
            public void onDnsSdServiceAvailable(String instanceName,
                                                String registrationType, WifiP2pDevice srcDevice) {

                Log.v("WifiDiscovery", "service discovery for " + instanceName);

                // check if instance matches with our service

                if (instanceName.equalsIgnoreCase(WifiNetworkService.SERVICE_INSTANCE)) {

                    Log.v("WifiDiscovery", "service available from " + srcDevice.deviceAddress);
                }
            }

        };


        mTxtListener = new DnsSdTxtRecordListener() {

            /**
             * A new TXT record is available. Pick up the advertised
             * buddy name.
             */
            @Override
            public void onDnsSdTxtRecordAvailable(
                    String fullDomainName, Map<String, String> record,
                    WifiP2pDevice device) {

//                String sender = record.get("s");
//                int c = Integer.parseInt(record.get("c"));

                Log.v("WifiDiscovery", "receive data from " + device.deviceAddress + " data " + record.keySet().toString() + ": " + record.values().toString());

                //create message
                NetworkMessage n = new NetworkMessage();
                n.sender = device.deviceAddress;
                n.clock = Short.parseShort(record.get("0"));

                //build clocks from data
                //TODO
                n.clocks = new HashMap<>();
                n.addresses = new HashMap<>();

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
        };

        //service request
        mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        //register listeners
        mManager.setDnsSdResponseListeners(mChannel, mResponseListener, mTxtListener);

        //create an handler for delayed tasks
        mHandler = new Handler();
    }

    public void discoverPeers(final int duration, final WifiP2pManager.ActionListener listener) {
        Log.v("WifiDiscovery", "discoverPeers for " + String.valueOf(duration));
        if (!isActive) {
            //start
            scanPeers(new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    isActive = true;
                    Log.v("WifiDiscovery", "start discovery");

                    //use handler for stopping after duration - 1 shot
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("WifiDiscovery", "discovery expired");
                            stopPeers(listener);
                        }
                    }, duration);

                }

                @Override
                public void onFailure(int error) {
                    Log.v("WifiDiscovery", "fail discovery");
                    //call listener
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                }
            });
        }

    }

    public void discovery(final int duration, final WifiP2pManager.ActionListener listener) {
        Log.v("WifiDiscovery", "discovery for " + String.valueOf(duration));
        if (!isActive) {
            //start
            start(new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    isActive = true;
                    Log.v("WifiDiscovery", "start discovery");

                    //use handler for stopping after duration - 1 shot
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("WifiDiscovery", "discovery expired");
                            stop(listener);
                        }
                    }, duration);

                }

                @Override
                public void onFailure(int error) {
                    Log.v("WifiDiscovery", "fail advertise");
                    //call listener
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                }
            });
        }


    }


    public void start(final WifiP2pManager.ActionListener listener) {
        if (!isActive) {
//            //register listeners
//            mManager.setDnsSdResponseListeners(mChannel, mResponseListener, mTxtListener);

            //add service request for discovery
            mManager.addServiceRequest(mChannel, mServiceRequest, new ActionListener() {

                @Override
                public void onSuccess() {
                    Log.v("WifiDiscovery", "Added service discovery request");
                }

                @Override
                public void onFailure(int error) {
                    Log.v("WifiDiscovery", "Failed adding service discovery request");
                    //call listener
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                }
            });
            mManager.discoverServices(mChannel, new ActionListener() {

                @Override
                public void onSuccess() {

                    Log.v("WifiDiscovery", "Service discovery initiated");
                    //call listener
                    if (listener != null) {
                        listener.onSuccess();
                    }
                }

                @Override
                public void onFailure(int error) {
                    Log.v("WifiDiscovery", "Service discovery failed");
                    //call listener
                    if (listener != null) {
                        listener.onFailure(error);
                    }

                }
            });


            isActive = true;
        }
    }


    public void stop(final WifiP2pManager.ActionListener listener) {
        if (isActive) {
            mManager.removeServiceRequest(mChannel, mServiceRequest, new ActionListener() {

                @Override
                public void onSuccess() {
                    isActive = false;
                    Log.v("WifiDiscovery", "Service discovery stopped");
                    //call listener
                    if (listener != null) {
                        listener.onSuccess();
                    }
                }

                @Override
                public void onFailure(int error) {
                    Log.v("WifiDiscovery", "Service discovery failed");
                    //call listener
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                }
            });
        }
    }

    public void scanPeers(final WifiP2pManager.ActionListener listener) {
        if (!isActive) {
            isActive = true;

            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Log.v("WifiDiscovery", "peers discovery started");
                    //call listener
                    if (listener != null) {
                        listener.onSuccess();
                    }
                }

                @Override
                public void onFailure(int error) {
                    Log.v("WifiDiscovery", "peers discovery failed");
                    //call listener
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                }
            });
        }
    }

    public void stopPeers(final WifiP2pManager.ActionListener listener) {
        if (isActive) {
            mManager.stopPeerDiscovery(mChannel, new ActionListener() {

                @Override
                public void onSuccess() {
                    isActive = false;
                    Log.v("WifiDiscovery", "peers discovery stopped");
                    //call listener
                    if (listener != null) {
                        listener.onSuccess();
                    }
                }

                @Override
                public void onFailure(int error) {
                    Log.v("WifiDiscovery", "peers discovery failed");
                    //call listener
                    if (listener != null) {
                        listener.onFailure(error);
                    }
                }
            });
        }
    }


}
