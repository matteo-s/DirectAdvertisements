/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.simulator.local;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import it.unitn.android.directadvertisements.network.NetworkMessage;
import it.unitn.android.directadvertisements.network.simulator.SimulatorService;

public class LocalSimulatorService implements SimulatorService {

    private List<LocalSimulatorNode> mNodes;

    private boolean isActive = false;

    public LocalSimulatorService(Context context, IBinder binder) {
        mNodes = new ArrayList<>();

        for (int i = 0; i < SIZE; i++) {
            //generate a random node address
            String address = generateRandomAddress();

            LocalSimulatorNode node = new LocalSimulatorNode(address, context, binder);

//            Intent intent = new Intent(context, LocalSimulatorNode.class);
//            // Start the service
//            context.startService(intent);

            mNodes.add(node);
        }


    }


    @Override
    public void init() {
        //reset clocks for all nodes in map
        for (LocalSimulatorNode node : mNodes) {
            node.reset();
        }
    }

    @Override
    public void destroy() {
        //destroy all nodes in map
        for (LocalSimulatorNode node : mNodes) {
            node.destroy();
            mNodes.remove(node);
        }
        isActive = false;
    }

    @Override
    public void activate() {
        Log.v("LocalSimulatorService", "activate");

        if (!isActive) {
            //start all nodes in map
            for (LocalSimulatorNode node : mNodes) {
                node.start();
            }
            isActive = true;
        }
    }

    @Override
    public void deactivate() {
        Log.v("LocalSimulatorService", "deactivate");

        //stop all nodes in map
        for (LocalSimulatorNode node : mNodes) {
            node.stop();
        }
        isActive = false;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void send(String address, NetworkMessage msg) {
        //ignore
    }

    @Override
    public void broadcast(NetworkMessage msg) {
        if (isActive) {
            //forward to all nodes in map
            for (LocalSimulatorNode node : mNodes) {
                node.receive(msg);
            }
        }
    }



    /*
    * Helpers
     */


    private String generateRandomAddress() {
        Random rand = new Random();
        byte[] macAddr = new byte[6];
        rand.nextBytes(macAddr);

        macAddr[0] = (byte) (macAddr[0] & (byte) 254);  //zeroing last 2 bytes to make it unicast and locally adminstrated

        StringBuilder sb = new StringBuilder(18);
        for (byte b : macAddr) {

            if (sb.length() > 0)
                sb.append(":");

            sb.append(String.format("%02x", b));
        }


        return sb.toString();
    }

}
