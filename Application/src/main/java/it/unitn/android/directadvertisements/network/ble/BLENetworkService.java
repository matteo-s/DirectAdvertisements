/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.ParcelUuid;

import it.unitn.android.directadvertisements.network.NetworkMessage;
import it.unitn.android.directadvertisements.network.NetworkNode;
import it.unitn.android.directadvertisements.network.NetworkService;
import it.unitn.android.directadvertisements.registry.NetworkRegistryUtil;

public class BLENetworkService implements NetworkService {

    /*
    * Constants
     */
    public static final ParcelUuid Service_UUID = ParcelUuid
            .fromString("0000b81d-0000-1000-8000-00805f9b34fb");


    /*
    * Data
     */
    private Context _context;
    private BluetoothAdapter _adapter;
    private boolean _available;
    private boolean _supported;


    /*
    * Bluetooth data
     */

    private String _address;
    private NetworkNode _node;

    public BLENetworkService(Context context) {
        _context = context;
        _adapter = null;
        _available = false;
        _supported = false;

        _address = "";
        _node = null;
    }

    @Override
    public String getNetwork() {
        return NetworkService.SERVICE_BLE;
    }

    @Override
    public boolean isSupported() {
        return _supported;
    }

    @Override
    public boolean isAvailable() {
        return _available;
    }

    public boolean isConfigured() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void init() {

    }

    public NetworkNode init2() {
        _adapter = ((BluetoothManager) _context.getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter();

        if (_adapter != null) {
            _supported = true;

            //check if active
            if (_adapter.isEnabled()) {
                //read address
                _address = readAddress(_adapter);

                //check for capabilities
                if (_adapter.isMultipleAdvertisementSupported()) {
                    //set as active
                    _available = true;

                    //return info
                    return info();
                } else {
                    //disable support
                    _supported = false;
                }

            } else {
                //set not active
                _available = false;
            }
        }

        return null;
    }

    @Override
    public void destroy() {
        if (_adapter != null) {
        }
    }

    public NetworkNode info() {
        if (_node == null && _adapter != null) {
            _node = new NetworkNode((byte) 0);
//            _node.setId(0);
            _node.address = _address;
            _node.name = _adapter.getName();
        }

        return _node;
    }


    @Override
    public void activate() {

    }

    @Override
    public void deactivate() {

    }


    @Override
    public void send(String address, NetworkMessage msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void send(int id, NetworkMessage msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void broadcast(NetworkMessage msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void multicast(String[] addresses, NetworkMessage msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void multicast(int[] ids, NetworkMessage msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void inquiry(String address) {
        throw new UnsupportedOperationException();
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



    /*
    * Helpers
     */

    protected String readAddress(BluetoothAdapter adapter) {
        return adapter.getAddress();
    }

}
