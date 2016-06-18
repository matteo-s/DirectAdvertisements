/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.simulator.local;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import it.unitn.android.directadvertisements.app.MessageKeys;
import it.unitn.android.directadvertisements.clocks.ClockService;
import it.unitn.android.directadvertisements.clocks.local.LocalClockService;
import it.unitn.android.directadvertisements.network.NetworkMessage;
import it.unitn.android.directadvertisements.network.NetworkNode;
import it.unitn.android.directadvertisements.network.NetworkServiceUtil;
import it.unitn.android.directadvertisements.network.simulator.SimulatorService;

public class LocalSimulatorNode extends Service {
    //    private Map<String, Integer> mClocks;
    private Map<Integer, NetworkNode> mNodes;
    private final String mAddress;
    private final int mId;
    private byte counter = 0;

    private Messenger bMessenger;
    boolean isBound = false;
    private boolean isActive = false;
    private ClockService mClockService;


    private volatile HandlerThread mHandlerThread;
    private ServiceHandler mServiceHandler;
    private LocalBroadcastManager mLocalBroadcastManager;
    private Messenger mMessenger;

    public LocalSimulatorNode() {
        this.mId = 0;
        this.mAddress = "";
        this.mNodes = new HashMap<>();
    }


    public LocalSimulatorNode(int id, String address, Context context, IBinder binder) {
        Log.v("LocalSimulatorNode", "create id " + String.valueOf(id) + " address " + address);

        this.mId = id;
        this.mAddress = address;
        this.mNodes = new HashMap<>();

        //bind
        this.bMessenger = new Messenger(binder);

        String tid = "LocalSimulatorNode." + mAddress + ".HandlerThread";
        Log.v("LocalSimulatorNode", "onCreate " + tid);

        mHandlerThread = new HandlerThread(tid);
        mHandlerThread.start();
        // An Android service handler is a handler running on a specific background thread.
        mServiceHandler = new ServiceHandler(mHandlerThread.getLooper());

//        // Get access to local broadcast manager
//        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        //messenger for ipc
        mMessenger = new Messenger(mServiceHandler);

        this.isBound = true;

    }

    /*
    * Simulator
     */

    public void start() {
        Log.v("LocalSimulatorNode", "start address " + mAddress + " is bound " + String.valueOf(isBound));

        if (isBound) {
            if (mClockService == null) {
                Log.v("LocalSimulatorNode", "create clock address " + mAddress);

                mClockService = new LocalClockService(this, mMessenger.getBinder());
            }

            //reset
            mClockService.reset();

            //start
            mClockService.start();
        }

    }


    public void stop() {
        Log.v("LocalSimulatorNode", "stop address " + mAddress);

        if (mClockService != null) {
            mClockService.stop();
        }
    }

    public void reset() {
        if (mClockService != null) {
            mClockService.stop();
            mClockService.reset();
        }
    }

    public void destroy() {
        if (mClockService != null) {
            mClockService.stop();
        }
        // Cleanup service before destruction
        mHandlerThread.quit();

        this.stopSelf();
    }


    public void send() {
        //simple random test for drop messages
        double p = Math.random();
        if (p > SimulatorService.LOSS) {
            Log.v("LocalSimulatorNode", "send address " + mAddress);

            //send update to mainService
            NetworkMessage n = new NetworkMessage();
            n.sender = mId;
            n.address = mAddress;
            n.clock = mClockService.get();

            //build clocks
            n.clocks = new HashMap<>();
            n.addresses = new HashMap<>();

            for (Integer i : mNodes.keySet()) {
                n.clocks.put(i, mNodes.get(i).clock);
                n.addresses.put(i, mNodes.get(i).address);
            }

            //directly send to service
            Message msg = Message.obtain(null, MessageKeys.CLOCK_RECEIVE, 0, 0);
            Bundle bundle = new Bundle();
            bundle.putSerializable("n", n);

            msg.setData(bundle);
            try {
                bMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    public void receive(NetworkMessage msg) {
        //simple random test for drop messages
        double p = Math.random();
        if (p > SimulatorService.LOSS) {
            Log.v("LocalSimulatorNode", "receive address " + mAddress);

            //check sender and clock
            int local = mClockService.get();
            if (msg.sender != mId && msg.clock >= local) {
                //update sender over map
                if (!mNodes.containsKey(msg.sender)) {
                    NetworkNode node = new NetworkNode(msg.sender);
                    node.address = msg.address;
                    node.clock = msg.clock;

                    mNodes.put(msg.sender, node);
                } else {
                    //overwrite because msg comes from device itself
                    if (mNodes.get(msg.sender).clock != msg.clock) {
                        mNodes.get(msg.sender).clock = msg.clock;
                    }
                }


                Iterator<Integer> iter = msg.clocks.keySet().iterator();
                while (iter.hasNext()) {
                    int i = iter.next();
                    //skip ourselves
                    if (i != mId) {
                        short c = msg.clocks.get(i);

                        //update if needed
                        if (mNodes.containsKey(i)) {
                            NetworkNode node = mNodes.get(i);

                            if (node.clock < c) {
                                node.clock = c;
                            }
                        } else {
                            NetworkNode node = new NetworkNode(i);
                            node.address = msg.addresses.get(i);
                            node.clock = c;


                            mNodes.put(i, node);
                        }
                    }
                }

                //update local clock if needed
                //will trigger update with new clocks map
                if (msg.clock > (local + 1)) {
                    //sync clock
                    mClockService.sync(msg.clock);
                }
            }
        }
    }

    private byte nextIdentifier() {
        return counter++;
    }



    /*
    * Service
     */

//    @Override
//    public void onCreate() {
//
//        super.onCreate();
//
//
//        // An Android handler thread internally operates on a looper.
//        String id = "LocalSimulatorNode." + mAddress + ".HandlerThread";
//        Log.v("LocalSimulatorNode", "onCreate " + id);
//
//        mHandlerThread = new HandlerThread(id);
//        mHandlerThread.start();
//        // An Android service handler is a handler running on a specific background thread.
//        mServiceHandler = new ServiceHandler(mHandlerThread.getLooper());
//
////        // Get access to local broadcast manager
////        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
//
//        //messenger for ipc
//        mMessenger = new Messenger(mServiceHandler);
//
//        this.isBound = true;
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.v("LocalSimulatorNode", "onStartCommand");
//        return this.START_NOT_STICKY;
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        Log.v("MainService", "onDestroy");
//
//        //stop
//        stop();
//
//        // Cleanup service before destruction
//        mHandlerThread.quit();
//    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }


    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        // Define how to handle any incoming messages here
        @Override
        public void handleMessage(Message msg) {

//            Log.v("ServiceHandler", "ServiceHandler handleMessage " + msg.what);

            if (msg.what == MessageKeys.CLOCK_INCREMENT) {
                Log.v("LocalSimulatorNode msg", "clock increment address " + mAddress);

                //send
                send();
            }

        }
    }

}
