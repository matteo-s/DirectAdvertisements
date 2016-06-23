/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import it.unitn.android.directadvertisements.clocks.ClockService;
import it.unitn.android.directadvertisements.clocks.ClockServiceUtil;
import it.unitn.android.directadvertisements.network.NetworkMessage;
import it.unitn.android.directadvertisements.network.NetworkNode;
import it.unitn.android.directadvertisements.network.NetworkService;
import it.unitn.android.directadvertisements.network.NetworkServiceUtil;
import it.unitn.android.directadvertisements.network.simulator.SimulatorService;
import it.unitn.android.directadvertisements.network.simulator.SimulatorServiceUtil;
import it.unitn.android.directadvertisements.network.wifi.WifiNetworkService;
import it.unitn.android.directadvertisements.registry.NetworkRegistry;
import it.unitn.android.directadvertisements.registry.NetworkRegistryUtil;

public class MainService extends Service {

    /*
    * Global
     */

    public static boolean running = false;


    /*
    * Network
     */

    private NetworkService mNetworkService;
    private NetworkRegistry mNetworkRegistry;
    //    private Map<String, NetworkNode> mNodes;
    private NetworkNode mNode;
    private String mAddress = "00:00:00:00:00";
    private int mId = 0;

    /*
    * Clock
     */
    private ClockService mClockService;


    /*
    * Simulator
     */
    private SimulatorService mSimulatorService;



    /*
    * App
     */

//    private Handler mHandler;
//    private final IBinder mBinder = new InnerBinder();
//
//    private Runnable timeoutRunnable;

    private volatile HandlerThread mHandlerThread;
    private ServiceHandler mServiceHandler;
    private LocalBroadcastManager mLocalBroadcastManager;
    private Messenger mMessenger;


    /*
    * Configuration
     */

    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);


    /*
    * Service
     */
    @Override
    public void onCreate() {

        super.onCreate();

        Log.v("MainService", "onCreate");

        // An Android handler thread internally operates on a looper.
        mHandlerThread = new HandlerThread("MainService.HandlerThread");
        mHandlerThread.start();
        // An Android service handler is a handler running on a specific background thread.
        mServiceHandler = new ServiceHandler(mHandlerThread.getLooper());

        // Get access to local broadcast manager
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        //messenger for ipc
        mMessenger = new Messenger(mServiceHandler);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Send empty message to background thread
//        mServiceHandler.sendEmptyMessageDelayed(0, 500);
        // or run code in background
//        mServiceHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                // Do something here in background!
//                // ...
//                // If desired, stop the service
////                stopSelf();
//                Log.v("MainService", "runnable run");
//            }
//        });


        // Keep service around "sticky"
        // Return "sticky" for services that are explicitly
        // started and stopped as needed by the app.
        Log.v("MainService", "onStartCommand");
//        return START_STICKY;
        return START_NOT_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v("MainService", "onDestroy");

        //stop
        stop();

        // Cleanup service before destruction
        mHandlerThread.quit();
    }

    // Binding is another way to communicate between service and activity
    @Override
    public IBinder onBind(Intent intent) {
        Log.v("MainService", "onBind");
//        return null;
        return mMessenger.getBinder();
    }


    // Define how the handler will process messages
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        // Define how to handle any incoming messages here
        @Override
        public void handleMessage(Message msg) {
            // ...
            // When needed, stop the service with
            // stopSelf();
            Log.v("ServiceHandler", "ServiceHandler handleMessage " + msg.what);

            Bundle bundle = msg.getData();

            switch (msg.what) {
                case MessageKeys.SERVICE_START:
                    final String network = (String) bundle.get("network");
                    final String deviceId = (String) bundle.get("deviceId");

                    Log.v("MainService msg", "service start " + network + " id " + deviceId);

                    //start
                    start(network, deviceId);
                    break;
                case MessageKeys.SERVICE_STOP:
                    Log.v("MainService msg", "service stop");

                    //stop
                    stop();
                    break;
                case MessageKeys.SERVICE_CLOSE:
                    //destroy service
                    destroy();
                    break;
                case MessageKeys.SIMULATOR_START:
                    Log.v("MainService msg", "simulator start");

                    //start
                    simulatorStart();
                    break;
                case MessageKeys.SIMULATOR_STOP:
                    Log.v("MainService msg", "simulator stop");

                    //stop
                    simulatorStop();
                    break;
                case MessageKeys.CLOCK_INCREMENT:
                    Log.v("MainService msg", "clock increment");
                    //increment clock
                    clockIncrement();
                    break;
                case MessageKeys.CLOCK_BROADCAST:
                    Log.v("MainService msg", "clock broadcast");
                    clockBroadcast();
                    break;
                case MessageKeys.CLOCK_RESET:
                    Log.v("MainService msg", "clock reset");
                    clockReset();
                    break;
                case MessageKeys.CLOCK_SYNC:
                    final short cs = bundle.getShort("c");
                    Log.v("MainService msg", "clock sync " + String.valueOf(cs));
                    //sync
                    clockSync(cs);
                    break;
                case MessageKeys.CLOCK_RECEIVE:
                    final NetworkMessage n = (NetworkMessage) bundle.getSerializable("n");
                    Log.v("MainService msg", "clock receive from " + n.sender);
                    //update
                    clockUpdate(n);
                    break;
                case MessageKeys.CLOCK_INQUIRY:
                    final String ai = (String) bundle.getSerializable("a");
                    Log.v("MainService msg", "clock inquiry for " + ai);
                    //inquiry
                    clockInquiry(ai);
                    break;
                case MessageKeys.CLOCK_SEND:
                    final String as = (String) bundle.getSerializable("a");
                    Log.v("MainService msg", "clock SEND TO " + as);
                    clockSend(as);
                    break;
                case MessageKeys.NETWORK_START:
                    Log.v("MainService msg", "network start callback");
                    break;
                case MessageKeys.NETWORK_STOP:
                    Log.v("MainService msg", "network stop callback");
                    break;
                case MessageKeys.NETWORK_ACTIVE:
                    Log.v("MainService msg", "network active callback");
                    break;
                case MessageKeys.NETWORK_INACTIVE:
                    Log.v("MainService msg", "network inactive callback");
                    break;
                case MessageKeys.NETWORK_INFO:
                    Log.v("MainService msg", "network info callback");
                    networkInfo();
                    break;
                case MessageKeys.NOTIFY_MESSAGE:
                    //send broadcast
                    final String message = (String) bundle.get("message");
                    mServiceHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // Send broadcast out with action filter and extras
                            Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                            intent.putExtra("message", message);
                            mLocalBroadcastManager.sendBroadcast(intent);
                        }
                    });
                    break;

                default:
                    super.handleMessage(msg);
            }


        }
    }

    /*
    * actions
     */
    private void networkInfo() {
        //fetch node from service
        if (mNetworkService != null) {
            NetworkNode nInfo = mNetworkService.info();

            if (nInfo != null) {

                //overwrite current stored info
                mNode.address = nInfo.address;
                mNode.name = nInfo.name;

                Log.v("MainService msg", "network address " + mNode.address);
                Log.v("MainService msg", "network name " + mNode.name);

                mAddress = mNode.address;

                //update ourselves in registry
                mNode.clock = mClockService.get();
                mNetworkRegistry.updateNode(mNode);

                //notify activity
                mServiceHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Send broadcast out with action filter and extras
                        Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                        intent.putExtra(MessageKeys.TYPE, MessageKeys.NETWORK_INFO);
                        intent.putExtra("name", mNode.name);
                        intent.putExtra("address", mNode.address);
                        mLocalBroadcastManager.sendBroadcast(intent);
                    }
                });


                //notify activity
                mServiceHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Send broadcast out with action filter and extras
                        Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                        intent.putExtra(MessageKeys.TYPE, MessageKeys.NOTIFY_UPDATE);

                        ArrayList<NetworkNode> nodes = mNetworkRegistry.getNodes();
                        intent.putExtra("nodes", nodes);
                        mLocalBroadcastManager.sendBroadcast(intent);
                    }
                });
            }
        }
    }

    private void clockIncrement() {
        mClockService = ClockServiceUtil.getService(this, mMessenger.getBinder());
        //read clock as integer
        final short c = mClockService.get();
        Log.v("MainService clock", "clock value " + String.valueOf(c));

        //send
        clockBroadcast();


        //update ourselves in registry
        mNode.clock = c;
        mNetworkRegistry.updateNode(mNode);

        //notify
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                // Send broadcast out with action filter and extras
                Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                intent.putExtra(MessageKeys.TYPE, MessageKeys.CLOCK_INCREMENT);
                intent.putExtra("c", c);
                mLocalBroadcastManager.sendBroadcast(intent);
            }
        });

        //notify activity
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                // Send broadcast out with action filter and extras
                Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                intent.putExtra(MessageKeys.TYPE, MessageKeys.NOTIFY_UPDATE);

                ArrayList<NetworkNode> nodes = mNetworkRegistry.getNodes();
                intent.putExtra("nodes", nodes);
                mLocalBroadcastManager.sendBroadcast(intent);
            }
        });
    }

    private void clockReset() {
        clockSync((short) 0);
    }

    private void clockSync(final short c) {
        mClockService = ClockServiceUtil.getService(this, mMessenger.getBinder());

        //set new value without resetting timer
        mClockService.set(c);

        //notify
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                // Send broadcast out with action filter and extras
                Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                intent.putExtra(MessageKeys.TYPE, MessageKeys.CLOCK_INCREMENT);
                intent.putExtra("c", c);
                mLocalBroadcastManager.sendBroadcast(intent);
            }
        });
    }

    private void clockBroadcast() {
        //build advertisement
        if (mClockService != null && mNetworkService != null && mNode != null) {
            NetworkMessage n = new NetworkMessage();
            n.sender = mNode.id;
            n.clock = mClockService.get();

            //build clocks
            n.clocks = new HashMap<>();
            n.addresses = new HashMap<>();

            //get nodes
            List<NetworkNode> nodes = mNetworkRegistry.getNodes();
            for (NetworkNode node : nodes) {
                n.clocks.put(node.id, node.clock);
                n.addresses.put(node.id, node.address);
            }

            //send
            mNetworkService.broadcast(n);

            //send to simulator
            if (mSimulatorService != null) {
                mSimulatorService.broadcast(n);
            }

        }
    }

    private void clockSend(String address) {
        //build advertisement
        if (mClockService != null && mNetworkService != null && mNode != null) {
            NetworkMessage n = new NetworkMessage();
            n.sender = mNode.id;
            n.clock = mClockService.get();

            //build clocks
            n.clocks = new HashMap<>();
            n.addresses = new HashMap<>();

            //get nodes
            List<NetworkNode> nodes = mNetworkRegistry.getNodes();
            for (NetworkNode node : nodes) {
                n.clocks.put(node.id, node.clock);
                n.addresses.put(node.id, node.address);
            }


            //send
            mNetworkService.send(address, n);

            //send to simulator
            if (mSimulatorService != null) {
                mSimulatorService.send(address, n);
            }

        }
    }

    private void clockUpdate(NetworkMessage msg) {
        Log.v("MainService", "clockUpdate");

        //check sender and clock
        int local = mClockService.get();
        boolean updated = false;

        if (msg.sender != mId) {
            Log.v("MainService", "clockUpdate exec from " + String.valueOf(msg.sender) + " clock " + String.valueOf(msg.clock));

            //notify network layer
            mNetworkService.receive(msg);

            //check if sender info is present
            if (msg.sender > 0) {
                //update sender over stored value
                if (mNetworkRegistry.hasNode(msg.sender)) {
                    //overwrite because msg comes from device itself
                    NetworkNode node = mNetworkRegistry.getNode(msg.sender);

                    if (node.clock != msg.clock) {
                        node.clock = msg.clock;
                        mNetworkRegistry.updateNode(node);

                        updated = true;
                    }
                } else {
                    //add as new
                    NetworkNode node = new NetworkNode(msg.sender);
                    node.address = msg.address;
                    node.clock = msg.clock;

                    mNetworkRegistry.addNode(node);
                    updated = true;
                }
            }

            //check for vector clock
            Iterator<Integer> iter = msg.clocks.keySet().iterator();
            while (iter.hasNext()) {
                int i = iter.next();
                //skip ourselves
                if (i != mId) {
                    short c = msg.clocks.get(i);

                    //update if needed
                    if (mNetworkRegistry.hasNode(i)) {
                        NetworkNode node = mNetworkRegistry.getNode(i);

                        if (node.clock < c) {
                            node.clock = c;
                            mNetworkRegistry.updateNode(node);

                            updated = true;
                        }
                    } else {
                        //add as new
                        NetworkNode node = new NetworkNode(i);
                        node.clock = msg.clocks.get(i);
                        //check if address is available - depends on service
                        if (msg.addresses.containsKey(i)) {
                            node.address = msg.addresses.get(i);
                        }

                        mNetworkRegistry.addNode(node);
                        updated = true;
                    }
                }
            }

            //update local clock if needed
            //will trigger update with new clocks map
            if (msg.clock > (local + 1)) {
                //sync clock
                mClockService.sync(msg.clock);
                updated = true;
            }

            if (updated) {
                Log.v("MainService", "clockUpdate notify");

                //notify activity
                mServiceHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Send broadcast out with action filter and extras
                        Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                        intent.putExtra(MessageKeys.TYPE, MessageKeys.NOTIFY_UPDATE);

                        ArrayList<NetworkNode> nodes = mNetworkRegistry.getNodes();
                        Log.v("MainService", "clockUpdate notify nodes " + String.valueOf(nodes.size()));

//                        Bundle bundle = new Bundle();
//                        bundle.putSerializable("nodes", nodes);

                        intent.putExtra("nodes", nodes);
                        mLocalBroadcastManager.sendBroadcast(intent);
                    }
                });
            }
        }
    }

    private void clockInquiry(String address) {
        //ask for inquiry on node
        if (mNetworkService != null) {
            //inquiry
            mNetworkService.inquiry(address);
        }
    }


    /*
    * handle services, can be called multiple times
     */
    protected void start(String network, String deviceId) {
        //local node
        int i = 0;
        try {
            i = Integer.parseInt(deviceId);
        } catch (Exception ex) {
            //reset
            i = 0;
        }
        Log.v("MainService", "MainService start node " + String.valueOf(i) + ", network " + network);

        mNode = new NetworkNode(i);

        //nodes
        if (mNetworkRegistry == null) {
            mNetworkRegistry = NetworkRegistryUtil.getRegistry();
        } else {
            //flush ?
            mNetworkRegistry.clear();
        }

        //add to registry
        mNetworkRegistry.addNode(mNode);

        //clock
        mClockService = ClockServiceUtil.getService(this, mMessenger.getBinder());

        //reset clock
        clockReset();

        //network
        mNetworkService = NetworkServiceUtil.getService(network, this, mMessenger.getBinder());

        //check if configured
        if (!mNetworkService.isConfigured()) {
            //get handle of system services
            switch (network) {
                case NetworkService.SERVICE_WIFI:
                    WifiP2pManager manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
                    WifiP2pManager.Channel channel = manager.initialize(this, getMainLooper(), null);
                    ((WifiNetworkService) mNetworkService).setup(manager, channel);
                    break;
                case NetworkService.SERVICE_BLE:
                    break;

            }
        }

        //init, if already initializated nothing happens
        mNetworkService.init();


        //start
        mNetworkService.activate();
        mClockService.start();

        //notify
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                // Send broadcast out with action filter and extras
                Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                intent.putExtra(MessageKeys.TYPE, MessageKeys.NOTIFY_MESSAGE);
                intent.putExtra("message", "MainService started");
                mLocalBroadcastManager.sendBroadcast(intent);
            }
        });

    }

    protected void stop() {
        Log.v("MainService", "MainService stop ");

        //clock
        if (mClockService != null) {
            mClockService.stop();
        }

        //network
        if (mNetworkService != null) {
            mNetworkService.deactivate();
        }

        //simulator
        if (mSimulatorService != null) {
            mSimulatorService.deactivate();
        }

        destroy();

//        //notify
//        mServiceHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                // Send broadcast out with action filter and extras
//                Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
//                intent.putExtra("message", "MainService stopped");
//                mLocalBroadcastManager.sendBroadcast(intent);
//            }
//        });
    }


    protected void simulatorStart() {
        Log.v("MainService", "MainService start simulator");
        mSimulatorService = SimulatorServiceUtil.getService(this, mMessenger.getBinder());

        //start
        mSimulatorService.activate();

    }

    protected void simulatorStop() {
        Log.v("MainService", "MainService stop simulator");
        if (mSimulatorService != null) {
            mSimulatorService.deactivate();
        }
    }


    protected void destroy() {
        this.stopSelf();
    }


//    // Define the callback for what to do when message is received
//    private BroadcastReceiver mAlarmReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String result = intent.getStringExtra("result");
//            Log.v("MainService", "mReceiver onReceive " + result);
//        }
//    };

}
