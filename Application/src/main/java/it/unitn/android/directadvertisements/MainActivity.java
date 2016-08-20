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

import java.io.File;
import java.util.List;

import it.unitn.android.directadvertisements.app.ActionKeys;
import it.unitn.android.directadvertisements.app.MainService;
import it.unitn.android.directadvertisements.app.MessageKeys;
import it.unitn.android.directadvertisements.app.NodesFragment;
import it.unitn.android.directadvertisements.log.LogServiceUtil;
import it.unitn.android.directadvertisements.network.NetworkNode;
import it.unitn.android.directadvertisements.network.NetworkService;
import it.unitn.android.directadvertisements.settings.SettingsService;
import it.unitn.android.directadvertisements.settings.SettingsServiceUtil;

public class MainActivity extends FragmentActivity {

    /*
 * Global
  */
    final static String NAMESPACE = "it.unitn.android.directadvertisements.app";

    boolean isBound = false;
    Messenger mMessenger;
    NodesFragment nodesFragment;
    ViewFlipper viewFlipper;
    SettingsService mSettings;

    int nodeCount = 0;
    String curActivity = "loading";
    boolean restricted = false;
    boolean isRunning = false;

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

        Log.v("MainService", "previous activity " + previous);

        //check if previous run
        if (previous.equals("main")) {
            String role = mSettings.getSetting("role", "slave");
            int id = Integer.parseInt(mSettings.getSetting("id", "0"));

            if (id != 0) {
                //set restricted
                restricted = true;
                mSettings.setSetting("restricted", Boolean.toString(restricted));
                mSettings.writeSettings();

                activityMain((id == 1), id, false);
            } else {
                //reset settings and load start
                mSettings.clearSetting("role");
                mSettings.clearSetting("activity");
                mSettings.clearSetting("id");
                mSettings.writeSettings();

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


    protected void activityMain(boolean master, final int nodeId, boolean autostart) {
        curActivity = "main";

        Log.v("MainActivity", "activityMain");
        final View mainLayout = (View) findViewById(R.id.main_layout);
        viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(mainLayout));

        TextView mainFieldClock = (TextView) findViewById(R.id.main_field_id);
        mainFieldClock.setText("Device " + String.valueOf(nodeId));

        //read mode
        boolean locked = Boolean.parseBoolean(mSettings.getSetting("locked", "false"));

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
                    startMainService(Integer.toString(nodeId));
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
            startMainService(Integer.toString(nodeId));
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

    public void startMainService(String deviceId) {
        Log.v("MainActivity", "startService");


//        Intent i = new Intent(this, MainService.class);
//        bindService(i, serviceConnection, Context.BIND_AUTO_CREATE);
//        // Start the service
//        startService(i);


//        isRunning = true;

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
        isRunning = false;

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

//        String path = LogServiceUtil.getLogger(this).path(component);
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

        String path = LogServiceUtil.getLogger(this).path(component);
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

}
