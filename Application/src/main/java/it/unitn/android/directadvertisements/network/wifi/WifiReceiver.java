/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.wifi;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import it.unitn.android.directadvertisements.app.MessageKeys;

public class WifiReceiver extends BroadcastReceiver {
    private WifiP2pManager mManager;
    private Channel mChannel;
    private Messenger mMessenger;
    private WifiPeerListener mPeerListener;
    private WifiInfoListener mInfoListener;
    private WifiP2pDevice mDevice;
    private WifiP2pDeviceList mPeerList;
    List<WifiP2pDevice> mPeers;
    boolean activeMode = false;
    private WifiP2pManager.ActionListener mListener;

    public WifiReceiver(WifiP2pManager manager, Channel channel, Messenger messenger) {
        mManager = manager;
        mChannel = channel;
        mMessenger = messenger;
        mPeerListener = new WifiPeerListener();
        mInfoListener = new WifiInfoListener();
        mDevice = null;
        mPeerList = null;
        mPeers = new ArrayList();
        activeMode = false;
        mListener = null;
    }


    public WifiReceiver(WifiP2pManager manager, Channel channel, Messenger messenger, boolean active, WifiP2pManager.ActionListener listener) {
        mManager = manager;
        mChannel = channel;
        mMessenger = messenger;
        mPeerListener = new WifiPeerListener();
        mInfoListener = new WifiInfoListener();
        mDevice = null;
        mPeerList = null;
        mPeers = new ArrayList();
        activeMode = active;
        mListener = listener;
    }

    public WifiP2pDevice getDevice() {
        return mDevice;
    }

    public List<WifiP2pDevice> getPeers() {
        return mPeers;
    }

    public WifiP2pDevice getPeer(String address) {
        if (mPeerList != null) {
            return mPeerList.get(address);
        } else {
            return null;
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                sendMessage(MessageKeys.NETWORK_ACTIVE, null);


            } else {
                // Wi-Fi P2P is not enabled
                sendMessage(MessageKeys.NETWORK_INACTIVE, null);

            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.v("WifiReceiver", "peers changed");

            mManager.requestPeers(mChannel, mPeerListener);


        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                Log.v("WifiReceiver", "network connected");

                // We are connected with the other device, request connection
                // info to find group owner IP

                mManager.requestConnectionInfo(mChannel, mInfoListener);
            } else {
                Log.v("WifiReceiver", "network disconnected");

                //disconnect
                if (mListener != null) {
                    //call network listener to signal operation
                    mListener.onFailure(0);
                }
            }


        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            mDevice = intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

            //send message
            sendMessage(MessageKeys.NETWORK_INFO, null);


        }
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


    private class WifiPeerListener implements WifiP2pManager.PeerListListener {

        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            //update
            mPeerList = peerList;
            mPeers = new ArrayList<>();
            mPeers.addAll(mPeerList.getDeviceList());

            for (WifiP2pDevice peer : mPeers) {
                String address = peer.deviceAddress;
                Log.v("WifiReceiver", "found peer " + peer.deviceName + " address " + address);

                if (activeMode) {
                    //send message to trigger inquiry on peer
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("a", address);
                    sendMessage(MessageKeys.CLOCK_INQUIRY, bundle);
                }
            }


        }
    }


    private class WifiInfoListener implements WifiP2pManager.ConnectionInfoListener {

        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {


            // InetAddress from WifiP2pInfo struct.
            InetAddress groupOwnerAddress = info.groupOwnerAddress;

            Log.v("WifiReceiver", "connection info group address " + groupOwnerAddress.getHostAddress());


            // After the group negotiation, we can determine the group owner.
            if (info.groupFormed && info.isGroupOwner) {
                // Do whatever tasks are specific to the group owner.
                // One common case is creating a server thread and accepting
                // incoming connections.

                Log.v("WifiReceiver", "group owner ");

            } else if (info.groupFormed) {
                // The other device acts as the client. In this case,
                // you'll want to create a client thread that connects to the group
                // owner.
                Log.v("WifiReceiver", "group client ");

            }


            if (mListener != null) {
                mListener.onSuccess();
            }

//            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
//
//                @Override
//                public void onSuccess() {
//                    //wifiReceiver will notify event
//                    Log.v("WifiReceiver", "group remove requested");
//
//                }
//
//                @Override
//                public void onFailure(int reason) {
//                    Log.v("WifiReceiver", "group remove fail");
//                    if (mListener != null) {
//                        mListener.onFailure(reason);
//                    }
//                }
//            });

        }
    }

}
