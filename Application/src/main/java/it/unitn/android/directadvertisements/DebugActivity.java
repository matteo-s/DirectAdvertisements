/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements;

import android.app.Activity;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import it.unitn.android.directadvertisements.app.MainService;
import it.unitn.android.directadvertisements.app.MessageKeys;
import it.unitn.android.directadvertisements.app.NodesFragment;
import it.unitn.android.directadvertisements.network.NetworkNode;
import it.unitn.android.directadvertisements.network.NetworkService;

public class DebugActivity extends FragmentActivity {

    /*
 * Global
  */
    boolean isBound = false;
    Messenger mMessenger;
    NodesFragment nodesFragment;


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


    protected void sendMessage(int key, Bundle bundle) {
        if (isBound) {
            // Create a Message
            // Note the usage of MSG_SAY_HELLO as the what value
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        setTitle(R.string.activity_main_title);

        Log.v("DebugActivity", "onCreate");

        Intent i = new Intent(this, MainService.class);
        bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);
        // Start the service
        startService(i);

        //bind inputs
        final EditText input_id = (EditText) findViewById(R.id.debug_input_id);


        //bind buttons

        final Button button = (Button) findViewById(R.id.debug_button_broadcast);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // send clock as broadcast via service
//                Bundle bundle = new Bundle();
//                bundle.putString("message", "test");

                sendMessage(MessageKeys.CLOCK_BROADCAST, null);

            }
        });
        final Switch toggleNetwork = (Switch) findViewById(R.id.debug_switch_network);

        final Switch toggleSimulator = (Switch) findViewById(R.id.debug_switch_simulator);
        toggleSimulator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startSimulator();
                } else {
                    stopSimulator();
                }
            }
        });

        final Switch toggleService = (Switch) findViewById(R.id.debug_switch_service);
        toggleService.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //fetch custom device id
                    String deviceId = input_id.getText().toString();

                    if (!deviceId.isEmpty()) {
                        //lock network toggle
                        toggleNetwork.setEnabled(false);

                        startMainService(deviceId);
                    } else {
                        //ignore and unlock
                        toggleService.setChecked(false);
                    }

                } else {
                    // The toggle is disabled
                    stopMainService();

                    //unlock network toggle
                    toggleNetwork.setEnabled(true);

                    //toggle also simulator
                    toggleSimulator.setChecked(false);

                }
            }
        });


        //setup fragments
        setupFragments();


    }

    private void setupFragments() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        nodesFragment = new NodesFragment();
        //add to container
//        transaction.add(R.id.debug_list_container, nodesFragment);
        transaction.replace(R.id.debug_list_container, nodesFragment);

        transaction.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for the particular broadcast based on ACTION string
        IntentFilter filter = new IntentFilter(MessageKeys.DEST_ACTIVITY);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
        // or `registerReceiver(testReceiver, filter)` for a normal broadcast
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener when the application is paused
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        // or `unregisterReceiver(testReceiver)` for a normal broadcast
    }

    // Define the callback for what to do when message is received
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int type = intent.getIntExtra("type", MessageKeys.NOTIFY_MESSAGE);

            switch (type) {
                case MessageKeys.CLOCK_UPDATE:
                    Log.v("DebugActivity", "receive clockUpdate");

                    short c = intent.getShortExtra("c", (short) 0);
                    TextView debugClock = (TextView) findViewById(R.id.debug_debug_clock);
                    debugClock.setText(String.valueOf(c));
                    break;
                case MessageKeys.NETWORK_INFO:
                    Log.v("DebugActivity", "receive networkInfo");

                    String name = intent.getStringExtra("name");
                    String address = intent.getStringExtra("address");

                    TextView debugName = (TextView) findViewById(R.id.debug_debug_name);
                    debugName.setText(name);

                    TextView debugAddress = (TextView) findViewById(R.id.debug_debug_address);
                    debugAddress.setText(address);

                    break;
                case MessageKeys.NOTIFY_UPDATE:
                    Log.v("DebugActivity", "receive update");

                    boolean hasNodes = intent.hasExtra("nodes");
                    Log.v("DebugActivity", "receive update has nodes " + String.valueOf(hasNodes));
                    short clock = intent.getShortExtra("clock", (short) 0);

                    if (hasNodes) {
                        List<NetworkNode> nodes = (List) intent.getSerializableExtra("nodes");
                        if (nodesFragment != null) {
                            nodesFragment.updateItems(clock ,nodes);
//                        nodesFragment.viewAdapter.addAll(nodes);
//                        //refresh
//                        nodesFragment.viewAdapter.notifyDataSetChanged();
                        }
                    }
//                default:
//                    String result = intent.getStringExtra("message");
//                    Toast.makeText(DebugActivity.this, result, Toast.LENGTH_SHORT).show();
//                    break;
            }


        }
    };


    public void startMainService(String deviceId) {
        Log.v("DebugActivity", "startService");

        Switch toggleNetwork = (Switch) findViewById(R.id.debug_switch_network);
        String service = NetworkService.SERVICE_WIFI;
        if (toggleNetwork.isChecked()) {
            //use blue
            service = NetworkService.SERVICE_BLE;
        }


        Bundle bundle = new Bundle();
        bundle.putString("network", service);
        bundle.putString("deviceId", deviceId);

//        // Construct our Intent specifying the Service
//        Intent i = new Intent(this, MainService.class);
//        // Add extras to the bundle
//        i.putExtra("foo", "bar");
//        // Start the service
//        startService(i);
        sendMessage(MessageKeys.SERVICE_START, bundle);
    }

    public void stopMainService() {
        Log.v("DebugActivity", "stopService");

        sendMessage(MessageKeys.SERVICE_STOP, null);
    }


    public void startSimulator() {
        Log.v("DebugActivity", "startSimulator");

        sendMessage(MessageKeys.SIMULATOR_START, null);
    }

    public void stopSimulator() {
        Log.v("DebugActivity", "stopSimulator");

        sendMessage(MessageKeys.SIMULATOR_STOP, null);
    }
}
