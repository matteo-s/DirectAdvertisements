/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

import java.io.File;
import java.util.List;
import java.util.Map;

import it.unitn.android.directadvertisements.app.MainService;
import it.unitn.android.directadvertisements.app.MessageKeys;
import it.unitn.android.directadvertisements.app.NodesFragment;
import it.unitn.android.directadvertisements.app.ProxyFragment;
import it.unitn.android.directadvertisements.app.ProxyView;
import it.unitn.android.directadvertisements.log.LogServiceFactory;
import it.unitn.android.directadvertisements.network.NetworkNode;
import it.unitn.android.directadvertisements.network.NetworkService;
import it.unitn.android.directadvertisements.settings.SettingsService;
import it.unitn.android.directadvertisements.settings.SettingsServiceUtil;

public class MainActivity extends FragmentActivity {

    /*
 * Global
  */
    final static String NAMESPACE = "it.unitn.android.directadvertisements.app";
    final static int REQUEST_ENABLE_BT = 102;

    boolean isBound = false;
    Messenger mMessenger;
    NodesFragment nodesFragment;
    ProxyFragment proxyFragment;
    ViewFlipper viewFlipper;
    SettingsService mSettings;

    BroadcastReceiver mCallback;

    int nodeCount = 0;
    String curActivity = "loading";
    boolean restricted = false;
    boolean isRunning = false;

    int mId;
    String mNetwork;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;

            // Create the Messenger object
            mMessenger = new Messenger(service);

            //check if loading activity, then move to start
            if (curActivity.equals("loading")) {
//                activityDispatch();
                //require update from service
                refreshMainService();

            }
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
//    //use intents
//    protected void sendMessage(int key, Bundle bundle) {
//        Intent i = new Intent(this, MainService.class);
//        if (key == MessageKeys.SERVICE_START) {
//            i.setAction(ActionKeys.ACTION_START);
//            final String network = (String) bundle.get("network");
//            final String deviceId = (String) bundle.get("deviceId");
//            i.putExtra("network", network);
//            i.putExtra("deviceId", deviceId);
//
//
//        } else if (key == MessageKeys.SERVICE_STOP) {
//            i.setAction(ActionKeys.ACTION_STOP);
//        }
//
//        startService(i);
//    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flipper);
        setTitle(R.string.activity_main_title);

        Log.v("MainActivity", "onCreate");

        Intent i = new Intent(this, MainService.class);
        // Start the service
        startService(i);
        //bind service
        bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);


        viewFlipper = (ViewFlipper) findViewById(R.id.viewflipper);
        viewFlipper.stopFlipping();

        nodesFragment = new NodesFragment();
        proxyFragment = new ProxyFragment();

        //setup fragments
        setupFragments();

        //read settings
        if (mSettings == null) {
            mSettings = SettingsServiceUtil.getService(this);
        }

        //load activity
        activityLoading();
    }

    @Override
    protected void onDestroy() {

        //kill service if not active
        if (!isRunning && isBound) {
            sendMessage(MessageKeys.SERVICE_CLOSE, null);
        }

        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }

        super.onDestroy();
    }

    protected void activityDispatch() {
        //check activity from settings
        String previous = mSettings.getSetting("activity", "loading");
        restricted = Boolean.parseBoolean(mSettings.getSetting("restricted", "false"));

        Log.v("MainActivity", "previous activity " + previous);

        //check if previous run
        if (previous.equals("main")) {
            String role = mSettings.getSetting("role", "slave");
            String network = mSettings.getSetting("network", "ble");
            int id = Integer.parseInt(mSettings.getSetting("id", "0"));

            if (id != 0) {
                //set restricted
                restricted = true;
                mSettings.setSetting("restricted", Boolean.toString(restricted));
                mSettings.writeSettings();

                //update switch
                final Switch startToggleNetwork = (Switch) findViewById(R.id.start_switch_network);
                startToggleNetwork.setChecked((!network.equals("ble")));

                mNetwork = network;
                mId = id;

                activityMain((id == 1), id, false);
            } else {
//                //reset settings and load start
//                mSettings.clearSetting("role");
//                mSettings.clearSetting("activity");
//                mSettings.clearSetting("id");
//                mSettings.writeSettings();

                //load start activity
                activityStart();
            }

        } else {
            //load start activity
            activityStart();
        }
    }


    protected void activityLoading() {
        curActivity = "loading";

        Log.v("MainActivity", "activityLoading");

        final View loadingLayout = (View) findViewById(R.id.start_layout);
        viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(loadingLayout));


    }

    protected void activityDebug() {
        Intent intent = new Intent(this, DebugActivity.class);
        startActivity(intent);
    }

    protected void activityStart() {
        curActivity = "start";

        Log.v("MainActivity", "activityStart");

        mId = 0;
        mNetwork = null;

        //reset settings
        mSettings.clearSetting("role");
        mSettings.clearSetting("activity");
        mSettings.clearSetting("id");
        mSettings.clearSetting("network");
        mSettings.clearSetting("network.proxy");

        mSettings.writeSettings();


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

//                activityDebug();

            }
        });

        final Switch startToggleNetwork = (Switch) findViewById(R.id.start_switch_network);
//        //disable
//        startToggleNetwork.setEnabled(false);

//        if (restricted) {
//            lockSettings();
//        } else {
//            unlockSettings();
//        }

        final Button startButtonLog = (Button) findViewById(R.id.start_button_log);
        startButtonLog.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                exportLog("app");
                viewLog("app");
            }
        });

        startButtonLog.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                shareLog("app");

                return true;
            }
        });


        //reset settings and load start
        mSettings.clearSetting("role");
        mSettings.clearSetting("activity");
        mSettings.clearSetting("id");
        mSettings.clearSetting("restricted");
        mSettings.writeSettings();

    }

    protected void activityGenerate() {
        curActivity = "generate";

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
                activityMain(true, 1, true);

            }
        });
    }

    protected void activityScan() {
        curActivity = "scan";

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
                    activityMain(false, nodeId, true);
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

    protected void activityProxy() {
        curActivity = "proxy";
        //override activity settings
        mSettings.setSetting("activity", "proxy");
        mSettings.writeSettings();

        Log.v("MainActivity", "activityProxy");
        final View proxyLayout = (View) findViewById(R.id.proxy_layout);
        viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(proxyLayout));


//        //get ble
//        final BluetoothAdapter mAdapter = ((BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE))
//                .getAdapter();
//
//        proxyCallback = new ScanCallback() {
//
//            @Override
//            public void onScanResult(int callbackType, ScanResult result) {
//                BluetoothDevice device = result.getDevice();
//                String address = device.getAddress();
//                String name = device.getName();
//
//                proxyFragment.addItem(address, name);
//
//            }
//
//        };
        final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //call us back with return code - disabled
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Create a BroadcastReceiver for ACTION_FOUND
        mCallback = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    String address = device.getAddress();
                    String name = device.getName();

                    Log.v("MainActivity", "bluetooth discovered " + address + " " + name);

//                    ParcelUuid[] uuids = device.getUuids();
//                    if (uuids != null) {
//                        for (ParcelUuid u : uuids) {
//                            if (u.equals(ProxyNetworkService.Service_UUID)) {
//                                proxyFragment.addItem(address, name);
//                            }
//                        }
//                    }

                    //add every device
                    proxyFragment.addItem(address, name);
                }
            }
        };


        //get button and bind
        final Button proxyButtonScan = (Button) findViewById(R.id.proxy_button_scan);
        proxyButtonScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (proxyButtonScan.getText().equals("Scan")) {
                    Log.v("MainActivity", "start bluetooth discovery");

                    // Register the BroadcastReceiver
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(mCallback, filter);
                    //start discovery
                    mAdapter.startDiscovery();

                    proxyButtonScan.setText("Stop");
                } else {
                    Log.v("MainActivity", "cancel bluetooth discovery");

                    mAdapter.cancelDiscovery();
                    //unregister
                    try {
                        unregisterReceiver(mCallback);
                    } catch (Exception ex) {
                    }
                    proxyButtonScan.setText("Scan");
                }
            }
        });
    }


    protected void activityMain(boolean master, final int nodeId, boolean autostart) {
        curActivity = "main";


        Log.v("MainActivity", "activityMain");
        final View mainLayout = (View) findViewById(R.id.main_layout);
        viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(mainLayout));

        TextView mainFieldClock = (TextView) findViewById(R.id.main_field_id);
        mainFieldClock.setText("Device " + String.valueOf(nodeId));

        //read mode
        boolean locked = Boolean.parseBoolean(mSettings.getSetting("locked", "false"));

        Switch toggleNetwork = (Switch) findViewById(R.id.start_switch_network);
        String network = NetworkService.SERVICE_BLE;
        if (toggleNetwork.isChecked()) {
            //use proxy
            network = NetworkService.SERVICE_PROXY;
        }

        if (mNetwork == null) {
            mNetwork = network;
        }
        mId = nodeId;

        //save to settings
        mSettings.setSetting("id", String.valueOf(mId));
        mSettings.setSetting("network", mNetwork);
        mSettings.writeSettings();

        //bind buttons
        final Button mainButtonService = (Button) findViewById(R.id.main_button_service);
        if (isRunning) {
            mainButtonService.setText("Stop");
        } else {
            mainButtonService.setText("Start");
        }

        mainButtonService.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isRunning) {
                    // stop
                    stopMainService();

//                    mainButtonService.setText("Start");

//                    if (!restricted) {
//                        //load start activity
//                        activityStart();
//                    }
                } else {

                    startMainService();
//                    mainButtonService.setText("Stop");
                }
            }
        });

        mainButtonService.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //clear restricted flag, stop and go back
                restricted = false;
                // stop
                stopMainService();
                //load start activity
                activityStart();
                return true;
            }
        });

//        //setup fragments
//        setupFragments();

        //save settings
        mSettings.setSetting("role", (master ? "master" : "slave"));
        mSettings.setSetting("id", Integer.toString(nodeId));
        mSettings.setSetting("activity", "main");
        mSettings.writeSettings();

        //start
        if (autostart) {
            startMainService();
        }


    }

    private void lockSettings() {
        //bind buttons
        final Button startButtonMaster = (Button) findViewById(R.id.start_button_master);

        final Button startButtonSlave = (Button) findViewById(R.id.start_button_slave);

        final Button startButtonDebug = (Button) findViewById(R.id.start_button_debug);
        startButtonDebug.setEnabled(false);

        final Switch startToggleNetwork = (Switch) findViewById(R.id.start_switch_network);
        startToggleNetwork.setEnabled(false);
    }

    private void unlockSettings() {
        //bind buttons
        final Button startButtonMaster = (Button) findViewById(R.id.start_button_master);

        final Button startButtonSlave = (Button) findViewById(R.id.start_button_slave);

        final Button startButtonDebug = (Button) findViewById(R.id.start_button_debug);
        startButtonDebug.setEnabled(true);

        final Switch startToggleNetwork = (Switch) findViewById(R.id.start_switch_network);
        startToggleNetwork.setEnabled(true);
    }


    private void setupFragments() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

//        nodesFragment = new NodesFragment();
        //add to container
//        transaction.add(R.id.list_container, nodesFragment);
        transaction.replace(R.id.main_list_container, nodesFragment);
        transaction.replace(R.id.proxy_list_container, proxyFragment);

        transaction.commit();
    }

    @Override
    protected void onResume() {
        Log.v("MainActivity", "onResume");

        super.onResume();
        // Register for the particular broadcast based on ACTION string
        IntentFilter filter = new IntentFilter(MessageKeys.DEST_ACTIVITY);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
        // or `registerReceiver(testReceiver, filter)` for a normal broadcast

        if (isRunning) {
            sendMessage(MessageKeys.REQUIRE_UPDATE, null);
        }
    }

    @Override
    protected void onPause() {
        Log.v("MainActivity", "onPause");

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
            final Button mainButtonService = (Button) findViewById(R.id.main_button_service);

            switch (type) {
                case MessageKeys.CLOCK_UPDATE:
                    Log.v("MainActivity", "receive clockUpdate");

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
                        short clock = intent.getShortExtra("clock", (short) 0);
                        TextView fieldClock = (TextView) findViewById(R.id.main_field_clock);
                        fieldClock.setText("clock " + String.valueOf(clock));

                        if (nodesFragment != null) {
                            nodesFragment.updateItems(clock, nodes);
//                        nodesFragment.viewAdapter.addAll(nodes);
//                        //refresh
//                        nodesFragment.viewAdapter.notifyDataSetChanged();
                        }
                    }
                    break;

                case MessageKeys.LOG_EXPORT:
                    Log.v("MainActivity", "receive log");
                    String path = intent.getStringExtra("path");
                    if (!path.isEmpty()) {
                        shareLog(path);
                    }
                    break;

                case MessageKeys.SERVICE_START:
                    Log.v("MainActivity", "receive start");
                    isRunning = true;
                    mainButtonService.setText("Stop");
                    break;
                case MessageKeys.SERVICE_STOP:
                    Log.v("MainActivity", "receive stop");
                    isRunning = false;
                    mainButtonService.setText("Start");
                    break;
                case MessageKeys.SERVICE_STATUS:
                    Log.v("MainActivity", "receive status");
                    isRunning = intent.getBooleanExtra("status", false);
                    String network = intent.getStringExtra("network");
                    int id = intent.getIntExtra("id", 0);
                    if (isRunning) {
                        mainButtonService.setText("Stop");
                    } else {
                        mainButtonService.setText("Start");
                    }

                    //check if loading activity, then move to start
                    if (curActivity.equals("loading")) {
                        if (isRunning) {
                            activityMain((id == 0), id, false);
                            sendMessage(MessageKeys.REQUIRE_UPDATE, null);

                        } else {
                            activityDispatch();
                        }
                    }
                    break;
//                default:
//                    String result = intent.getStringExtra("message");
//                    Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
//                    break;
            }


        }
    };

    public void refreshMainService() {
        Log.v("MainActivity", "refreshService");
        sendMessage(MessageKeys.SERVICE_STATUS, null);
    }

    //    public void startMainService(String network, String deviceId) {
    public void startMainService() {

        Log.v("MainActivity", "startService");

        boolean canStart = (mNetwork != null && mId > 0);

        //check for proxy
        if (mNetwork != null) {
            if (mNetwork.equals("proxy")) {
                String proxy = mSettings.getSetting("network.proxy", "");

                if (proxy.isEmpty()) {
                    canStart = false;

                    activityProxy();
                }
            }
        }


//        Intent i = new Intent(this, MainService.class);
//        bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);
//        // Start the service
//        startService(i);


//        isRunning = true;

        //use ble
//        String service = NetworkService.SERVICE_BLE;

        if (canStart) {
            Bundle bundle = new Bundle();
            bundle.putString("network", mNetwork);
            bundle.putString("deviceId", String.valueOf(mId));

            //fetch all network properties
            Map<String, String> settings = mSettings.getSettings();
            for (String k : settings.keySet()) {
                if (k.startsWith("network.")) {
                    bundle.putString(k, settings.get(k));
                }
            }

//        // Construct our Intent specifying the Service
//        Intent i = new Intent(this, MainService.class);
//        // Add extras to the bundle
//        i.putExtra("foo", "bar");
//        // Start the service
//        startService(i);
            sendMessage(MessageKeys.SERVICE_START, bundle);
        }
    }

    public void stopMainService() {
        Log.v("MainActivity", "stopService");
//        isRunning = false;

        sendMessage(MessageKeys.SERVICE_STOP, null);

//        //unbind
//        unbindService(serviceConnection);


    }

//
//    public void startSimulator() {
//        Log.v("MainActivity", "startSimulator");
//
//        sendMessage(MessageKeys.SIMULATOR_START, null);
//    }
//
//    public void stopSimulator() {
//        Log.v("MainActivity", "stopSimulator");
//
//        sendMessage(MessageKeys.SIMULATOR_STOP, null);
//    }

    public void viewLog(String component) {
        Log.v("MainActivity", "viewLog");


        Intent intent = new Intent(this, LogActivity.class);
        startActivity(intent);

    }

    public void exportLog(String component) {
        Log.v("MainActivity", "exportLog");

//        Bundle bundle = new Bundle();
//        bundle.putString("component", component);
//
//        sendMessage(MessageKeys.LOG_EXPORT, bundle);

//        String path = LogServiceFactory.getLogger(this).path(component);
        File l = new File(this.getFilesDir(), "app.log");

        Log.v("MainActivity", "exportLog file size " + String.valueOf(l.length()));

        Uri contentUri = FileProvider.getUriForFile(this,
                "it.unitn.android.directadvertisements.MainActivity", l);

        Intent viewIntent = new Intent();
        viewIntent.setAction(Intent.ACTION_VIEW);
        viewIntent.setType("text/plain");
//        viewIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        viewIntent.setDataAndType(contentUri, "text/plain");
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(viewIntent, "Open with"));


    }


    public void shareLog(String component) {

        String path = LogServiceFactory.getLogger(this).path(component);
        File l = new File(path);

        Uri contentUri = FileProvider.getUriForFile(this,
                "it.unitn.android.directadvertisements.MainActivity", l);

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share with"));


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


    public void proxySelect(ProxyView item) {
        String device = item.address;
        Log.v("MainActivity", "proxy selected device " + device);

        final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
        //cancel discovery if running
        mAdapter.cancelDiscovery();

        //unregister
        if (mCallback != null) {
            try {
                unregisterReceiver(mCallback);
            } catch (Exception ex) {
            }
        }

        //save to settings
        mSettings.setSetting("network.proxy", device);
        mSettings.writeSettings();

        //go to main activity
        int nodeId = Integer.parseInt(mSettings.getSetting("id", "0"));
        activityMain(false, nodeId, true);


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
                    activityMain(false, nodeId, true);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


//    private void showNotification(short clock, NodeView node) {
//        short missing = (short) Math.min((clock - node.clock), 0);
//
//        Intent resultIntent = new Intent(this, MainActivity.class);
//        // The stack builder object will contain an artificial back stack for the
//        // started Activity.
//        // This ensures that navigating backward from the Activity leads out of
//        // your application to the Home screen.
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//        // Adds the back stack for the Intent (but not the Intent itself)
//        stackBuilder.addParentStack(MainActivity.class);
//        // Adds the Intent that starts the Activity to the top of the stack
//        stackBuilder.addNextIntent(resultIntent);
//        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//
//
//        Bitmap icon = BitmapFactory.decodeResource(getResources(),
//                R.drawable.ic_launcher);
//
//        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
//                .setContentTitle("DirectAdvertisement")
//                .setTicker("DirectAdvertisement")
//                .setContentText("Node alert for " + node.id + ":" + node.name + " missing for " + String.valueOf(missing))
//                .setSmallIcon(R.drawable.ic_launcher)
//                .setLargeIcon(
//                        Bitmap.createScaledBitmap(icon, 128, 128, false))
//                .setContentIntent(pendingIntent);
//
//        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        // mId allows you to update the notification later on.
//        mNotificationManager.notify(node.id, mBuilder.build());
//
//    }

}
