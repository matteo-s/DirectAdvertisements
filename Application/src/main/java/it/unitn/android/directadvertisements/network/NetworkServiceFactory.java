/**
 * Created by mat on 2016
 */

package it.unitn.android.directadvertisements.network;

import android.content.Context;
import android.os.IBinder;

import it.unitn.android.directadvertisements.app.ServiceConnector;
import it.unitn.android.directadvertisements.network.ble.BLENetworkService;
import it.unitn.android.directadvertisements.network.wifi.WifiNetworkService;

public class NetworkServiceFactory {


//    private static NetworkService _service;
//    private static boolean _available = false;
//    private static byte counter = 0;

    public static NetworkService getService(String network, Context context) {
//        if (_service != null) {
//            if (!_service.getNetwork().equals(network)) {
//                //stop
//                _service.destroy();
//
//                //clear
//                _service = null;
//            }
//        }
//
//        if (_service == null) {
        NetworkService _service = null;
        switch (network) {
            case NetworkService.SERVICE_WIFI:
                _service = new WifiNetworkService(context, new ServiceConnector(context));
                break;
            case NetworkService.SERVICE_BLE:
                _service = new BLENetworkService(context, new ServiceConnector(context));
                break;
        }
        return _service;
//        }
//        return _service;
    }


//    /*
//    * Node counter
//     */
//    public static byte nextIdentifier() {
//        return counter++;
//    }
//
//    /*
//     * Events handling
//     */
//
//
//    public static void receive(NetworkMessage msg) {
//
//    }





    /*
    * Data
     */
//    Context _context;
//    private Map<String, NetworkNode> _nodes;
//    private int _clock;


//    public NetworkServiceFactory(Context context) {
//        this._context = context;
//        this._nodes = new LinkedHashMap<>();
//        this._clock = -1;
//    }
//
//
//    /*
//    * Nodes
//     */
//
//    public NetworkNode registerNode(NetworkNode n) {
//        if (!_nodes.containsKey(n.getAddress())) {
//            _nodes.put(n.getAddress(), n);
//            return n;
//        } else {
//            NetworkNode node = _nodes.get(n.getAddress());
//            //update data
//            node.setName(n.getName());
//            node.setAddress(n.getAddress());
//            node.setClock(n.getClock());
//            return node;
//        }
//    }
//
//    public NetworkNode registerNode(String address) {
//        if (!_nodes.containsKey(address)) {
//            //create new
//            NetworkNode node = new NetworkNode();
//            //set data
//            node.setName(address);
//            node.setAddress(address);
//            node.setClock(-1);
//
//            //add
//            _nodes.put(address, node);
//
//            //return
//            return node;
//        } else {
//            return _nodes.get(address);
//        }
//    }
//
//
//    public boolean hasNode(NetworkNode n) {
//        return _nodes.containsKey(n.getAddress());
//    }
//
//    public boolean hasNode(String address) {
//        return _nodes.containsKey(address);
//    }
//
//    public List<NetworkNode> getNodes() {
//        return new LinkedList(_nodes.entrySet());
//    }
//
//    public void removeNode(NetworkNode n) {
//        if (_nodes.containsKey(n.getAddress())) {
//            _nodes.remove(n.getAddress());
//        }
//    }
//
//    public void removeNode(String address) {
//        if (_nodes.containsKey(address)) {
//            _nodes.remove(address);
//        }
//    }


}
