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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import it.unitn.android.directadvertisements.clocks.ClockService;
import it.unitn.android.directadvertisements.clocks.ClockServiceFactory;
import it.unitn.android.directadvertisements.log.LogService;
import it.unitn.android.directadvertisements.log.LogServiceUtil;
import it.unitn.android.directadvertisements.network.NetworkMessage;
import it.unitn.android.directadvertisements.network.NetworkNode;
import it.unitn.android.directadvertisements.network.NetworkService;
import it.unitn.android.directadvertisements.network.NetworkServiceFactory;
import it.unitn.android.directadvertisements.network.wifi.WifiNetworkService;
import it.unitn.android.directadvertisements.registry.NetworkRegistry;
import it.unitn.android.directadvertisements.registry.NetworkRegistryUtil;
import it.unitn.android.directadvertisements.settings.SettingsService;
import it.unitn.android.directadvertisements.settings.SettingsServiceUtil;

public class MainService extends Service {

    /*
    * Global
     */

    public boolean RUNNING = false;
    private String uuid;

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
    * Log
     */
    private LogService mLogger;

    /*
    * App
     */
    SettingsService mSettings;


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

        Log.v("MainService", "onCreate " + this);

        // An Android handler thread internally operates on a looper.
        mHandlerThread = new HandlerThread("MainService.HandlerThread");
        mHandlerThread.start();
        // An Android service handler is a handler running on a specific background thread.
        mServiceHandler = new ServiceHandler(mHandlerThread.getLooper());

        // Get access to local broadcast manager
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        //messenger for ipc
        mMessenger = new Messenger(mServiceHandler);

        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }

        Log.v("MainService", "onCreate uuid " + uuid);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("MainService", "onStartCommand");

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


//        //read action
//        if (intent.getAction().equals(ActionKeys.ACTION_START)) {
//            Log.v("MainService", "action start");
//
//            final String network = (String) intent.getStringExtra("network");
//            final String deviceId = (String) intent.getStringExtra("deviceId");
//
//            start(network, deviceId);
//        } else if (intent.getAction().equals(ActionKeys.ACTION_STOP)) {
//            Log.v("MainService", "action stop");
//
//            stop();
//
//            destroy();
//        }


        //read settings
        if (mSettings == null) {
            mSettings = SettingsServiceUtil.getService(this);
        }

        //check if previously running
        if (!RUNNING) {
            boolean previous = Boolean.parseBoolean(mSettings.getSetting("running", "false"));
            if (previous) {
                int id = Integer.parseInt(mSettings.getSetting("id", "0"));
                String network = mSettings.getSetting("network", "ble");

                //start
                start(network, String.valueOf(id));
            }

        }

        // Keep service around "sticky"
        // Return "sticky" for services that are explicitly
        // started and stopped as needed by the app.
        return START_STICKY;
//        return START_NOT_STICKY;
//        return START_REDELIVER_INTENT;

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
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v("MainService", "onUnbind");
        //call destroy
        destroy();

        return super.onUnbind(intent);
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
                case MessageKeys.SERVICE_STATUS:
                    Log.v("MainService msg", "service status");

                    //status
                    status();
                    break;
                case MessageKeys.SERVICE_CLOSE:
                    //destroy service
                    destroy();
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
                case MessageKeys.LOG_EXPORT:
                    Log.v("MainService msg", "export log callback");
                    final String comp = (String) bundle.getSerializable("component");
                    exportLog(comp);
                    break;
                case MessageKeys.LOG_CLEAR:
                    Log.v("MainService msg", "clear log callback");
                    clearLog();
                    break;
                case MessageKeys.REQUIRE_UPDATE:
                    Log.v("MainService msg", "require update callback");
                    notifyUpdate();
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
                        short c = mClockService.get();

                        intent.putExtra("nodes", nodes);
                        intent.putExtra("clock", c);

                        mLocalBroadcastManager.sendBroadcast(intent);
                    }
                });
            }
        }
    }

    private void clockIncrement() {
//        mClockService = ClockServiceFactory.getService(this);
        //read clock as integer
        final short c = mClockService.get();
        Log.v("MainService clock", "clock value " + String.valueOf(c));

        //logger
        mLogger.info("clock", String.valueOf(c));


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
                short c = mClockService.get();

                intent.putExtra("nodes", nodes);
                intent.putExtra("clock", c);

                mLocalBroadcastManager.sendBroadcast(intent);
            }
        });
    }

    private void clockReset() {
        clockSync((short) 0);
    }

    private void clockSync(final short c) {
//        mClockService = ClockServiceFactory.getService(this, mMessenger.getBinder());

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
            int maxId = mNode.id;

            //get nodes
            List<NetworkNode> nodes = mNetworkRegistry.getNodes();
            for (NetworkNode node : nodes) {
                n.clocks.put(node.id, node.clock);
                n.addresses.put(node.id, node.address);

                if (node.id > maxId) {
                    maxId = node.id;
                }

            }

            StringBuilder vector = new StringBuilder();
            for (int i = 1; i <= maxId; i++) {
                if (n.clocks.containsKey(i)) {
                    vector.append(Short.toString(n.clocks.get(i))).append(" ");
                } else {
                    vector.append("0").append(" ");
                }
            }
            //logger
            mLogger.info("nodes", vector.toString());

            //send
            mNetworkService.broadcast(n);


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


        }
    }

    private void clockUpdate(NetworkMessage msg) {
        Log.v("MainService", "clockUpdate");
        if (mClockService != null) {
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
            /*
            * Criteria for update
            * 1. received bigger than local
            * 2. reset clock interval if updated
             */
                //will trigger update with new clocks map
                if (msg.clock > local) {
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
                            short c = mClockService.get();
                            Log.v("MainService", "clockUpdate notify nodes " + String.valueOf(nodes.size()));

                            intent.putExtra("nodes", nodes);
                            intent.putExtra("clock", c);

                            mLocalBroadcastManager.sendBroadcast(intent);
                        }
                    });
                }
            }
        }
    }

    private void clockInquiry(String address) {
//        //ask for inquiry on node
//        if (mNetworkService != null) {
//            //inquiry
//            mNetworkService.inquiry(address);
//        }
    }


    /*
    * handle services, can be called multiple times
     */
    protected void start(String network, String deviceId) {
        //local node
        mId = 0;
        try {
            mId = Integer.parseInt(deviceId);
        } catch (Exception ex) {
            //reset
            mId = 0;
        }
        Log.v("MainService", "MainService " + uuid + " start node " + String.valueOf(mId) + ", network " + network);

        //logger
        if (mLogger == null) {
            mLogger = LogServiceUtil.getLogger(this);
        }

        //clock
        if (mClockService == null) {
            mClockService = ClockServiceFactory.getService(this);
        }
        //reset clock
        clockReset();

        //start clock from 1 instead of zero
        if (mClockService.get() == 0) {
            mClockService.set((short) 1);
        }
        mClockService.start();

        mNode = new NetworkNode(mId);
        mNode.clock = mClockService.get();

        //nodes
        if (mNetworkRegistry == null) {
            mNetworkRegistry = NetworkRegistryUtil.getRegistry();
        } else {
            //flush ?
            mNetworkRegistry.clear();
        }

        //add to registry
        mNetworkRegistry.addNode(mNode);


        //network
        if (mNetworkService == null || !mNetworkService.getNetwork().equals(network)) {
            mNetworkService = NetworkServiceFactory.getService(network, this);
        }

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

        //init, if already initialized nothing happens
        mNetworkService.init();


        //start, will cause bind
        mNetworkService.activate();

        RUNNING = true;

        //save
        mSettings.setSetting("running", Boolean.toString(RUNNING));
        mSettings.setSetting("network", network);
        mSettings.setSetting("id", deviceId);

        mSettings.writeSettings();


        //notify
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                // Send broadcast out with action filter and extras
                Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                intent.putExtra(MessageKeys.TYPE, MessageKeys.SERVICE_START);
                intent.putExtra("message", "MainService started");
                mLocalBroadcastManager.sendBroadcast(intent);
            }
        });

    }

    protected void status() {
        Log.v("MainService", "MainService status ");

        //notify
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                // Send broadcast out with action filter and extras
                Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                intent.putExtra(MessageKeys.TYPE, MessageKeys.SERVICE_STATUS);
                intent.putExtra("status", RUNNING);
                if (RUNNING) {
                    intent.putExtra("network", mNetworkService.getNetwork());
                    intent.putExtra("id", mId);
                }
                mLocalBroadcastManager.sendBroadcast(intent);
            }
        });
    }

    protected void stop() {
        Log.v("MainService", "MainService stop ");
        RUNNING = false;

        //save
        mSettings.setSetting("running", Boolean.toString(RUNNING));
        //leave node info in settings for next run
//        mSettings.clearSetting("network");
//        mSettings.clearSetting("id");

        mSettings.writeSettings();

        //clock
        if (mClockService != null) {
            mClockService.stop();


        }

        //network
        if (mNetworkService != null) {
            mNetworkService.deactivate();
        }

        //notify
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                // Send broadcast out with action filter and extras
                Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                intent.putExtra(MessageKeys.TYPE, MessageKeys.SERVICE_STOP);
                intent.putExtra("message", "MainService started");
                mLocalBroadcastManager.sendBroadcast(intent);
            }
        });


//        destroy();

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

    /*
    * notify
     */
    protected void notifyUpdate() {
        if (mNetworkRegistry != null && mClockService != null) {
            //notify activity
            mServiceHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Send broadcast out with action filter and extras
                    Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                    intent.putExtra(MessageKeys.TYPE, MessageKeys.NOTIFY_UPDATE);

                    ArrayList<NetworkNode> nodes = mNetworkRegistry.getNodes();
                    short c = mClockService.get();

                    intent.putExtra("nodes", nodes);
                    intent.putExtra("clock", c);

                    mLocalBroadcastManager.sendBroadcast(intent);
                }
            });
        }
    }

    /*
    * Log
     */
    protected void exportLog(String component) {
        if (mLogger == null) {
            mLogger = LogServiceUtil.getLogger(this);

        }

        Log.v("MainService", "MainService export log for " + component);

        final String path = mLogger.path(component);


        //notify
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                // Send broadcast out with action filter and extras
                Intent intent = new Intent(MessageKeys.DEST_ACTIVITY);
                intent.putExtra(MessageKeys.TYPE, MessageKeys.LOG_EXPORT);
                intent.putExtra("path", path);
                mLocalBroadcastManager.sendBroadcast(intent);
            }
        });


    }

    protected void clearLog() {
        if (mLogger != null) {
            Log.v("MainService", "MainService clear log");
            mLogger.clear();
        }
    }

    protected void destroy() {
        Log.v("MainService", "MainService destroy");

        boolean canStop = true;

        //log
        if (mLogger != null) {
            mLogger.destroy();
        }

        //clock
        if (mClockService != null) {
            mClockService.destroy();
        }

        //network
        if (mNetworkService != null) {
            mNetworkService.destroy();
        }

        if (canStop) {
            Log.v("MainService", "MainService destroy, stop self");
            this.stopSelf();
        }
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
