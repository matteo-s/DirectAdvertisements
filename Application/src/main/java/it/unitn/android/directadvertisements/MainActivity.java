/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.List;

import it.unitn.android.directadvertisements.app.MainService;
import it.unitn.android.directadvertisements.app.MessageKeys;
import it.unitn.android.directadvertisements.app.NodesFragment;
import it.unitn.android.directadvertisements.network.NetworkNode;
import it.unitn.android.directadvertisements.network.NetworkService;

public class MainActivity extends FragmentActivity {

    /*
 * Global
  */
    final static String NAMESPACE = "it.unitn.android.directadvertisements.app";

    boolean isBound = false;
    Messenger mMessenger;
    NodesFragment nodesFragment;
    ViewFlipper viewFlipper;

    int nodeCount = 0;

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
        setContentView(R.layout.activity_flipper);
        setTitle(R.string.activity_main_title);

        Log.v("MainActivity", "onCreate");

        Intent i = new Intent(this, MainService.class);
        bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);
        // Start the service
        startService(i);


        viewFlipper = (ViewFlipper) findViewById(R.id.viewflipper);
        viewFlipper.stopFlipping();


        //load start activity
        activityStart();

        nodesFragment = new NodesFragment();
        //setup fragments
        setupFragments();

    }


    protected void activityDebug() {
        Intent intent = new Intent(this, DebugActivity.class);
        startActivity(intent);
    }

    protected void activityStart() {
//        setContentView(R.layout.activity_start);
//        setTitle(R.string.activity_main_title);


        Log.v("MainActivity", "activityStart");
        final View startLayout = (View) findViewById(R.id.start_layout);
        viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(startLayout));


        //bind buttons
        final Button startButtonMaster = (Button) findViewById(R.id.start_button_master);
        startButtonMaster.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

//                //load main activity
//                activityMain(true, 1);

                //load qr generator
                activityGenerate();
            }
        });

        final Button startButtonSlave = (Button) findViewById(R.id.start_button_slave);
        startButtonSlave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //load qr generator
                activityScan();
            }
        });

        final Button startButtonDebug = (Button) findViewById(R.id.start_button_debug);
        startButtonDebug.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                activityDebug();

            }
        });

        final Switch startToggleNetwork = (Switch) findViewById(R.id.start_switch_network);
        //disable
        startToggleNetwork.setEnabled(false);

    }

    protected void activityGenerate() {
//        setContentView(R.layout.activity_qr_generate);
//        setTitle(R.string.activity_main_title);

        Log.v("MainActivity", "activityGenerate");
        final View qrGenerateLayout = (View) findViewById(R.id.qr_generate_layout);
        viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(qrGenerateLayout));

        nodeCount = 1;

        final TextView qrGenerateFieldId = (TextView) findViewById(R.id.qr_generate_field_id);
        qrGenerateFieldId.setText("Device " + String.valueOf(nodeCount));

        final ImageView qrGenerateImage = (ImageView) findViewById(R.id.qr_generate_image);

        //bind buttons
        final Button qrGenerateButtonNext = (Button) findViewById(R.id.qr_generate_button_next);
        qrGenerateButtonNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                nodeCount++;
                qrGenerateFieldId.setText("Device " + String.valueOf(nodeCount));

                //generate
                qrGenerate(qrGenerateImage, nodeCount);

            }
        });

        final Button qrGenerateButtonStart = (Button) findViewById(R.id.qr_generate_button_start);
        qrGenerateButtonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //load main activity
                activityMain(true, 1);

            }
        });
    }

    protected void activityScan() {
//        setContentView(R.layout.activity_qr_scan);
//        setTitle(R.string.activity_main_title);

        Log.v("MainActivity", "activityScan");
        final View qrScanLayout = (View) findViewById(R.id.qr_scan_layout);
        viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(qrScanLayout));

        nodeCount = 1;

        final EditText qrScanInputId = (EditText) findViewById(R.id.qr_scan_input_id);

        //bind buttons
        final Button qrScanButtonInput = (Button) findViewById(R.id.qr_scan_button_input);
        qrScanButtonInput.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String value = qrScanInputId.getText().toString();
                if (!value.isEmpty()) {
                    int nodeId = Integer.parseInt(value);

                    //load main activity
                    activityMain(false, nodeId);
                }
            }
        });

        final Button qrScanButtonScan = (Button) findViewById(R.id.qr_scan_button_scan);
        qrScanButtonScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                qrScan();
            }
        });
    }


    protected void activityMain(boolean master, int nodeId) {
//        setContentView(R.layout.activity_main);
//        setTitle(R.string.activity_main_title);

        Log.v("MainActivity", "activityMain");
        final View mainLayout = (View) findViewById(R.id.main_layout);
        viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(mainLayout));

        TextView mainFieldClock = (TextView) findViewById(R.id.main_field_id);
        mainFieldClock.setText("Device " + String.valueOf(nodeId));


        //bind buttons
        final Button mainButtonService = (Button) findViewById(R.id.main_button_service);
        mainButtonService.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // stop
                stopMainService();

                //load start activity
                activityStart();
            }
        });

//        //setup fragments
//        setupFragments();

        //start
        startMainService(Integer.toString(nodeId));


    }

    private void setupFragments() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

//        nodesFragment = new NodesFragment();
        //add to container
//        transaction.add(R.id.list_container, nodesFragment);
        transaction.replace(R.id.main_list_container, nodesFragment);

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
                case MessageKeys.CLOCK_INCREMENT:
                    Log.v("MainActivity", "receive clockIncrement");

                    short c = intent.getShortExtra("c", (short) 0);
                    TextView mainFieldClock = (TextView) findViewById(R.id.main_field_clock);
                    mainFieldClock.setText("clock " + String.valueOf(c));
                    break;
                case MessageKeys.NETWORK_INFO:
                    Log.v("MainActivity", "receive networkInfo");

                    String name = intent.getStringExtra("name");
                    String address = intent.getStringExtra("address");


                    break;
                case MessageKeys.NOTIFY_UPDATE:
                    Log.v("MainActivity", "receive update");

                    boolean hasNodes = intent.hasExtra("nodes");
                    Log.v("MainActivity", "receive update has nodes " + String.valueOf(hasNodes));
                    if (hasNodes) {
                        List<NetworkNode> nodes = (List) intent.getSerializableExtra("nodes");
                        if (nodesFragment != null) {
                            nodesFragment.updateItems(nodes);
//                        nodesFragment.viewAdapter.addAll(nodes);
//                        //refresh
//                        nodesFragment.viewAdapter.notifyDataSetChanged();
                        }
                    }
//                default:
//                    String result = intent.getStringExtra("message");
//                    Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
//                    break;
            }


        }
    };


    public void startMainService(String deviceId) {
        Log.v("MainActivity", "startService");


        //use ble
//        String service = NetworkService.SERVICE_BLE;
        Switch toggleNetwork = (Switch) findViewById(R.id.start_switch_network);
        String service = NetworkService.SERVICE_BLE;
        if (!toggleNetwork.isChecked()) {
            //use blue
            service = NetworkService.SERVICE_WIFI;
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
        Log.v("MainActivity", "stopService");

        sendMessage(MessageKeys.SERVICE_STOP, null);
    }


    public void startSimulator() {
        Log.v("MainActivity", "startSimulator");

        sendMessage(MessageKeys.SIMULATOR_START, null);
    }

    public void stopSimulator() {
        Log.v("MainActivity", "stopSimulator");

        sendMessage(MessageKeys.SIMULATOR_STOP, null);
    }


    public void qrGenerateRun(final ImageView qrCodeImage, final int nodeId) {
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Generating");
        progress.setMessage("Please wait...");

        // create thread to avoid ANR Exception
        Thread t = new Thread(new Runnable() {
            public void run() {
                progress.show();
                try {
                    synchronized (this) {
                        // runOnUiThread method used to do UI task in main thread.
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                qrGenerate(qrCodeImage, nodeId);
                            }
                        });

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                progress.dismiss();


            }
        });
        t.start();
    }


    public void qrGenerate(ImageView qrCodeImage, final int nodeId) {

        String content = MainActivity.NAMESPACE + "-" + String.valueOf(nodeId);

        BitMatrix result;


        try {


            result = new MultiFormatWriter().encode(content,
                    BarcodeFormat.QR_CODE, 500, 500, null);

            int w = result.getWidth();
            int h = result.getHeight();
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                int offset = y * w;
                for (int x = 0; x < w; x++) {
                    pixels[offset + x] = result.get(x, y) ?
                            getResources().getColor(R.color.black) :
                            getResources().getColor(R.color.white);
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, 500, 0, 0, w, h);

            qrCodeImage.setImageBitmap(bitmap);

        } catch (IllegalArgumentException iae) {
            // Unsupported format

        } catch (WriterException e) {

        }

    }

    public void qrScan() {
        new IntentIntegrator(this).initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                activityScan();
            } else {
                String content = result.getContents();
                Log.v("MainActivity", "scan result " + content);

                if (content.startsWith(MainActivity.NAMESPACE)) {
                    String value = content.replace(MainActivity.NAMESPACE + "-", "");
                    int nodeId = Integer.parseInt(value);

                    //load
                    activityMain(false, nodeId);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
