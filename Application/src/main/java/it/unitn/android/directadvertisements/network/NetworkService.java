/**
 * Created by mat on 2016
 */

package it.unitn.android.directadvertisements.network;

public interface NetworkService {
   /*
    * Constants
     */

    public static final String SERVICE_BLE = "ble";
    public static final String SERVICE_WIFI = "wifi";
    public static final String SERVICE_PROXY = "proxy";

    // public static final int REQUEST_ENABLE_BLE = 11;
    // public static final int REQUEST_ENABLE_WIFI = 12;


    /*
    * Methods
     */
    public String getNetwork();

    public boolean isSupported();

    public boolean isConfigured();

    public boolean isAvailable();

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

    //unicast send
    public void send(String address, NetworkMessage msg);

    public void send(int id, NetworkMessage msg);

    //broadcast send
    public void broadcast(NetworkMessage msg);

    //multicast send
    public void multicast(String[] addresses, NetworkMessage msg);

    public void multicast(int[] ids, NetworkMessage msg);


    public void inquiry(String address);

    public void inquiry(int id);

    public void receive(NetworkMessage msg);

    /*
    * Info
     */
    public void getIdentifier();

    public NetworkNode info();

}
