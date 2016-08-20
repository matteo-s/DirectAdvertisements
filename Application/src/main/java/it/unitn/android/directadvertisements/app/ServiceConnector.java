/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class ServiceConnector {


    Messenger mMessenger = null;

    boolean isBound;
    private Context mContext;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;

            // Create the Messenger object
            mMessenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // unbind or process might have crashes
            mMessenger = null;
            isBound = false;
        }
    };


    public ServiceConnector(Context context) {
        mContext = context;
//        //bind service - delegated to client
//        bindService();
    }

    /**
     * Method used for binding with the service
     */
    public boolean bindService() {
        if (!isBound) {
            Intent i = new Intent(mContext, MainService.class);

            return mContext.bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            return true;
        }
    }

    public void unbindService() {
        if (isBound) {
            mContext.unbindService(serviceConnection);
            isBound = false;
        }
    }

    public boolean isBound() {
        return isBound;
    }

    public void sendMessage(int key, Bundle bundle) {
        if (isBound) {
            // Create a Message
            Message msg = Message.obtain(null, key, 0, 0);

//        // Create a bundle with the data
//        Bundle bundle = new Bundle();
//        bundle.putString("hello", "world");

            // Set the bundle data to the Message
            if (bundle != null) {
                msg.setData(bundle);
            }
            // Send the Message to the Service (in another process)
            try {
                mMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

}
