/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.proxy;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import it.unitn.android.directadvertisements.app.MessageKeys;
import it.unitn.android.directadvertisements.app.ServiceConnector;
import it.unitn.android.directadvertisements.log.LogService;
import it.unitn.android.directadvertisements.log.LogServiceFactory;

public class ProxyClient {
    private BluetoothAdapter mAdapter;

    private boolean isActive = false;
    private boolean isConnected = false;
    private Handler mHandler;

    private ServiceConnector mService = null;
    private LogService mLogger;

    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;

    private ConnectedThread mConnection;

    public ProxyClient(BluetoothAdapter adapter, ServiceConnector serviceConnector, LogService logger) {
        this.mAdapter = adapter;
        this.mService = serviceConnector;

        this.mDevice = null;
        this.mSocket = null;
        this.mConnection = null;

        //create an handler for delayed tasks
        mHandler = new Handler();

        this.mLogger = logger;

        isActive = false;
    }

    public boolean isConnected() {
        return isActive;
    }

    public void connect(String address) {
        Log.v("ProxyClient", "connect to " + address);
        if (mDevice != null) {
            mDevice = null;
        }

        mDevice = mAdapter.getRemoteDevice(address);
        // Cancel discovery because it will slow down the connection
        mAdapter.cancelDiscovery();

        BluetoothSocket tmp = null;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = mDevice.createRfcommSocketToServiceRecord(ProxyNetworkService.Service_UUID);
        } catch (IOException e) {
        }

        mSocket = tmp;


        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mSocket.connect();

            mConnection = new ConnectedThread(mSocket);
            //start thread
            mConnection.start();

            isActive = true;
        } catch (IOException connectException) {
            isActive = false;

            // Unable to connect; close the socket and get out
            try {
                mSocket.close();
            } catch (IOException closeException) {
            }
        }

        Log.v("ProxyClient", "connected " + String.valueOf(isActive));

        mLogger.info("proxy", "connected to " + address + " result: " + String.valueOf(isActive));

        if (!isActive) {
            //stop service
            mService.sendMessage(MessageKeys.SERVICE_STOP, null);

        }

    }


    public void disconnect() {
        Log.v("ProxyClient", "disconnect");


        isActive = false;
        isConnected = false;

        if (mConnection != null) {
            //stop handler
            mConnection.cancel();

            mConnection = null;
        }

        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException closeException) {
            }

            mSocket = null;
        }

        if (mDevice != null) {
            mDevice = null;
        }

        mLogger.info("proxy", "disconnected");


    }

    public void reset() {
        Log.v("ProxyClient", "reset");
        String address = "";
        if (isActive && isConnected) {
            address = mDevice.getAddress();
        }
        disconnect();

        if (!address.isEmpty()) {
            connect(address);
        }


    }

    public void send(ProxyNetworkMessage m) {
        if (isActive && isConnected) {

            StringBuilder vector = new StringBuilder();
            for (int i = 1; i <= ProxyNetworkMessage.SLOTS; i++) {
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

            byte[] bytes = m.buildData();

            Log.v("ProxyClient", "write bytes length " + String.valueOf(bytes.length) + ": " + ProxyNetworkMessage.byteArrayToString(bytes));


            mConnection.write(bytes);
        }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                //reset
                reset();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            isConnected = true;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int length; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    length = mmInStream.read(buffer);

                    if (length > 0) {
                        //parse bytes
                        byte[] bytes = Arrays.copyOf(buffer, length);

                        //dump
                        Log.v("ProxyClient", "receive data on socket " + ProxyNetworkMessage.byteArrayToString(bytes));

                        //get message from service data bytes
                        ProxyNetworkMessage n = ProxyNetworkMessage.parseData(bytes);

                        StringBuilder vector = new StringBuilder();
                        for (int i = 1; i <= ProxyNetworkMessage.SLOTS; i++) {
                            if (n.clocks.containsKey(i)) {
                                vector.append(Short.toString(n.clocks.get(i)));
                            } else {
                                vector.append("0");
                            }
                            vector.append(" ");
                        }


                        Log.v("ProxyClient", "received msg : " + vector.toString());

                        //log to file
                        mLogger.info("proxy", "received msg from " + String.valueOf(n.sender) + " " + vector.toString());

                        //directly send to service
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("n", n);

                        mService.sendMessage(MessageKeys.CLOCK_RECEIVE, bundle);
                    }

                } catch (IOException e) {
                    //reset
                    reset();
                    break;
                }
            }


        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                //reset
                reset();
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
}
