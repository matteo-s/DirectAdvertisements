/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.List;
import java.util.UUID;

import it.unitn.android.directadvertisements.app.ServiceConnector;
import it.unitn.android.directadvertisements.log.LogService;

public class ProxyClient {


    private BluetoothAdapter mAdapter;

    private boolean isActive = false;
    private Handler mHandler;

    private Context mContext;
    private ServiceConnector mService = null;
    private LogService mLogger;

    private BluetoothDevice mDevice;
    //save address explicitly
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public static final String SERVICE_UUID = "0000b81d-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_UUID = "00001110-0000-1000-8000-00805f9b34fb";

//    private ConnectedThread mConnection;

    public ProxyClient(BluetoothAdapter adapter, ServiceConnector serviceConnector, Context context, LogService logger) {
        this.mAdapter = adapter;
        this.mService = serviceConnector;

        //bind context
        this.mContext = context;

        this.mDevice = null;
        this.mBluetoothGatt = null;
//        this.mConnection = null;

        //create an handler for delayed tasks
        mHandler = new Handler();

        this.mLogger = logger;

    }

    public boolean isConnected() {
        return mConnectionState == STATE_CONNECTED;
    }

    public void connect(final String address) {
        Log.v("ProxyClient", "connect to " + address);
        if (mDevice != null || mBluetoothGatt != null) {
            mDevice = null;
            mBluetoothGatt = null;
        }

        // Previously connected device.  Try to reconnect.
        if (mDevice != null
                && mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {

            Log.v("ProxyClient", "reuse connection");

            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
            } else {
                //reset
                reset();
            }
        } else {
            if (mDevice != null || mBluetoothGatt != null) {
                disconnect();
            }

            //connect
            mDevice = mAdapter.getRemoteDevice(address);

            Log.v("ProxyClient", "init connection");

            // We want to directly connect to the device, so we are setting the autoConnect
            // parameter to false.
            mBluetoothDeviceAddress = address;

            mBluetoothGatt = mDevice.connectGatt(mContext, false, mGattCallback);
            mConnectionState = STATE_CONNECTING;

        }
    }

    public void disconnect() {
        Log.v("ProxyClient", "disconnect");

        mBluetoothDeviceAddress = null;


//        if (mConnection != null) {
////            //stop handler
//            mConnection.cancel();
//
//            mConnection = null;
//        }

        if (mBluetoothGatt != null) {
            try {
                //close
                mBluetoothGatt.close();

                //disconnect
                mBluetoothGatt.disconnect();
            } catch (Exception ex) {
                //ignore
            }

            mBluetoothGatt = null;
        }


        if (mDevice != null) {
            mDevice = null;
        }

        mLogger.info("ProxyClient", "disconnected");
    }


    public void reset() {
        Log.v("ProxyClient", "reset");
        String address = mBluetoothDeviceAddress;
        
        disconnect();

        if (!address.isEmpty()) {
            connect(address);
        }


    }


    public void send(BLENetworkMessage m) {
        Log.v("ProxyClient", "request write data isConnected " + String.valueOf(mConnectionState));

        if (isConnected()) {

            StringBuilder vector = new StringBuilder();
            for (int i = 1; i <= BLENetworkMessage.SLOTS; i++) {
                if (m.clocks.containsKey(i)) {
                    vector.append(Short.toString(m.clocks.get(i)));
                } else {
                    vector.append("0");
                }
                vector.append(" ");
            }

            Log.v("ProxyClient", "write data : " + vector.toString());

            //log to file
            mLogger.info("proxy", "send msg from " + String.valueOf(m.sender) + " " + vector.toString());

            byte[] bytes = m.buildManufacturerBytes();

            Log.v("ProxyClient", "write bytes length " + String.valueOf(bytes.length) + ": " + BLENetworkMessage.byteArrayToString(bytes));


            //write directly
            BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(SERVICE_UUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(
                        UUID.fromString(CHARACTERISTIC_UUID));
                if(characteristic != null) {

                    characteristic.setValue(bytes);

                    if (mBluetoothGatt.writeCharacteristic(characteristic) == false) {
                        Log.v("ProxyClient", "write characteristic error");
                    } else {
                        Log.v("ProxyClient", "write characteristic success");
                    }
                } else {
                    Log.v("ProxyClient", "characteristic not found");
                    List<BluetoothGattCharacteristic> chs = service.getCharacteristics();

                    for(BluetoothGattCharacteristic ch : chs) {
                        Log.v("ProxyClient","available service ch "+ch.getUuid().toString());
                    }
                }
            } else {
                Log.v("ProxyClient","service not found");

                //dump services
                List<BluetoothGattService> services = mBluetoothGatt.getServices();
                for(BluetoothGattService sc : services) {
                    Log.v("ProxyClient","available gatt service "+sc.getUuid().toString());
                }

            }
        } else if(mConnectionState == STATE_DISCONNECTED && mBluetoothDeviceAddress != null) {
            connect(mBluetoothDeviceAddress);

        }
    }


    //ble


    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

//    //connection
//    private class ConnectedThread extends Thread {
//        private final BluetoothGatt mmGatt;
//
//        public ConnectedThread(BluetoothGatt gatt) {
//            mmGatt = gatt;
//        }
//
//
//
//    }


    //callback
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {

                @Override
                public void onConnectionStateChange(
                        BluetoothGatt gatt,
                        int status,
                        int newState) {

                    Log.v("ProxyClient", "connection state change: " + String.valueOf(newState));

                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        mConnectionState = STATE_CONNECTED;

                        Log.v("ProxyClient", "Connected to GATT server.");
                        // Attempts to discover services after successful connection.
                        mBluetoothGatt.discoverServices();
                        Log.v("ProxyClient", "Attempting to start service discovery");

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        mConnectionState = STATE_DISCONNECTED;
                        Log.v("ProxyClient", "Disconnected from GATT server.");
                    }

                }

                @Override
                // New services discovered
                public void onServicesDiscovered(
                        BluetoothGatt gatt,
                        int status) {
                    Log.v("ProxyClient", "service discovered status: " + String.valueOf(status));

                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(
                        BluetoothGatt gatt,
                        BluetoothGattCharacteristic characteristic,
                        int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        byte[] bytes = characteristic.getValue();
                        Log.v("ProxyClient", "characteristic read " + characteristic.getUuid() + " : " + BLENetworkMessage.byteArrayToString(bytes));
                    }
                }


                @Override
                public void onCharacteristicWrite(
                        BluetoothGatt gatt,
                        BluetoothGattCharacteristic characteristic,
                        int status) {

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        byte[] bytes = characteristic.getValue();
                        Log.v("ProxyClient", "characteristic write " + characteristic.getUuid() + " : " + BLENetworkMessage.byteArrayToString(bytes));

                    }
                }

                @Override
                public void onCharacteristicChanged(
                        BluetoothGatt gatt,
                        BluetoothGattCharacteristic characteristic) {
                }
            };
}
