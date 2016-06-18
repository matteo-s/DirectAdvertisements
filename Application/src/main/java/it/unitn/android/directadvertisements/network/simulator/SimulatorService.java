/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.simulator;

import it.unitn.android.directadvertisements.network.NetworkMessage;

public interface SimulatorService {

    public final static int SIZE = 5;
    public final static double LOSS = 0.1;
    public final static int INTERVAL_MIN = 5000;
    public final static int INTERVAL_MAX = 9000;




 /*
 * Handling
  */

    public void init();

    public void destroy();

 /*
 * Status
  */

    public void activate();

    public void deactivate();

    public boolean isActive();


 /*
 * Messages
  */

    public void send(String address, NetworkMessage msg);

    public void broadcast(NetworkMessage msg);


}
