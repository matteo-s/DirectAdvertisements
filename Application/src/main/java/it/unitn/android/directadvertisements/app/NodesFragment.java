/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.app;

import android.R.color;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import it.unitn.android.directadvertisements.MainActivity;
import it.unitn.android.directadvertisements.R;
import it.unitn.android.directadvertisements.network.NetworkNode;
import it.unitn.android.directadvertisements.settings.SettingsService;
import it.unitn.android.directadvertisements.settings.SettingsServiceUtil;

public class NodesFragment extends ListFragment {
    private List<NodeView> mItems;
    private short mClock;
    public NodeViewAdapter viewAdapter;


    public void updateItems(short clock, List<NetworkNode> items) {
        Log.v("NodesFragment", "updateItems clock " + String.valueOf(clock) + " items " + String.valueOf(items.size()));

        //save current items
        List<NodeView> cItems = new LinkedList<>();
        cItems.addAll(mItems);

        //get settings
        SettingsService settings = SettingsServiceUtil.getService();

        mClock = clock;
//        //update
//        viewAdapter.clear();

        //build display nodeView
        mItems = new LinkedList<>();
        for (NetworkNode node : items) {
            NodeView w = new NodeView();
            w.id = node.id;
            w.address = node.address;
            w.clock = node.clock;
            //check name
            w.name = node.name;
            //look in settings
            if (settings != null) {
                w.name = settings.getSetting("node." + Integer.toString(node.id) + ".name", node.name);
            }

            //default white
            w.color = color.white;

//delegated to refresh
//            if (w.clock >= mClock) {
//                w.color = color.holo_green_light;
//            } else if (w.clock == (mClock - 1)) {
//                w.color = color.holo_orange_light;
//            } else if (w.clock < (mClock - 1)) {
//                w.color = color.holo_red_light;
//            }

            Log.v("NodesFragment", "updateItems item " + String.valueOf(w.id) + " clock " + String.valueOf(w.clock));

            //look in settings if hidden
            boolean hide = false;
            if (settings != null) {
                hide = Boolean.parseBoolean(settings.getSetting("node." + Integer.toString(node.id) + ".hide", "false"));
            }

            //show anyway but in a disabled state
            w.hide = hide;

            //delegated to refresh
//            if (hide) {
//                //change color
//                w.color = color.darker_gray;
//            }

            //look in settings if monitored for alerting
            boolean alert = false;
            if (settings != null) {
                alert = Boolean.parseBoolean(settings.getSetting("node." + Integer.toString(node.id) + ".alert", "false"));
            }

            w.alert = alert;

            //disabled, moved to service otherwise works only when activity is displayed
//            if (alert) {
//                //check if node is behind clock
//                short threshold = (short) Math.max((clock - 1), 0);
//                if (w.clock < threshold) {
//                    //send notification for node
//                    showNotification(clock, w);
//                }
//            }


            //add to list
            mItems.add(w);
        }

        //refresh view
        refreshItems();
    }

    public void refreshItems() {
        Log.v("NodesFragment", "refresh items " + String.valueOf(mItems.size()));

        //update colors
        for (NodeView w : mItems) {
            if (w.clock >= mClock) {
                w.color = color.holo_green_light;
            } else if (w.clock == (mClock - 1)) {
                w.color = color.holo_orange_light;
            } else if (w.clock < (mClock - 1)) {
                w.color = color.holo_red_light;
            }

            if (w.hide) {
                //change color
                w.color = color.darker_gray;
            }
        }


        //sort
        Collections.sort(mItems, NodeView.ASCENDING_COMPARATOR);


        viewAdapter.clear();

        viewAdapter.addAll(mItems);
        viewAdapter.notifyDataSetChanged();

        Log.v("NodesFragment", "refreshed count " + String.valueOf(viewAdapter.getCount()));

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v("NodesFragment", "onCreate super");

        Resources resources = getResources();

        // initialize the items list
        mItems = new LinkedList<>();

        //use a dedicated list for viewAdapter content
        viewAdapter = new NodeViewAdapter(getActivity(), new LinkedList<NodeView>());
        Log.v("NodesFragment", "onCreate viewAdapter");

        // initialize and set the list adapter
        setListAdapter(viewAdapter);
        Log.v("NodesFragment", "onCreate setAdapter");

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // remove the dividers from the ListView of the ListFragment
//        getListView().setDivider(null);

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0,
                                           View arg1,
                                           int position, long id) {

                // retrieve theListView item
                NodeView item = mItems.get(position);

                if (item != null) {
                    editNode(item);
                }


                return true;
            }
        });
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // retrieve theListView item
        NodeView item = mItems.get(position);

        String msg = "node " + String.valueOf(item.id) + " clock " + String.valueOf(item.clock);

        // do something
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }


    private void editNode(final NodeView node) {
        Context context = getActivity();

        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.activity_prompt, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptsView);

        final TextView editLabel = (TextView) promptsView.findViewById(R.id.prompt_label_field);
        editLabel.setText("Rename " + String.valueOf(node.id));

        final EditText editName = (EditText) promptsView.findViewById(R.id.prompt_edit_field);
        editName.setText(node.name);

        final Switch hideSwitch = (Switch) promptsView.findViewById(R.id.prompt_hide_field);
        hideSwitch.setChecked(node.hide);

        final Switch alertSwitch = (Switch) promptsView.findViewById(R.id.prompt_alert_field);
        alertSwitch.setChecked(node.alert);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String name = editName.getText().toString();
                                boolean hide = hideSwitch.isChecked();
                                boolean alert = alertSwitch.isChecked();

                                //update
                                node.name = name;
                                node.hide = hide;
                                node.alert = alert;

                                //add to settings
                                SettingsService settings = SettingsServiceUtil.getService();
                                if (settings != null) {
                                    if (name.isEmpty()) {
                                        settings.clearSetting("node." + Integer.toString(node.id) + ".name");
                                    } else {
                                        settings.setSetting("node." + Integer.toString(node.id) + ".name", node.name);
                                    }
                                    if (hide) {
                                        settings.setSetting("node." + Integer.toString(node.id) + ".hide", Boolean.toString(hide));
                                    } else {
                                        settings.clearSetting("node." + Integer.toString(node.id) + ".hide");
                                    }
                                    if (alert) {
                                        settings.setSetting("node." + Integer.toString(node.id) + ".alert", Boolean.toString(alert));
                                    } else {
                                        settings.clearSetting("node." + Integer.toString(node.id) + ".alert");
                                    }

                                    settings.writeSettings();
                                }

                                //force refresh
                                refreshItems();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    private void showNotification(short clock, NodeView node) {
        short missing = (short) Math.max((clock - node.clock), 0);
        Log.v("NodesFragment", "show notification node " + String.valueOf(node.id) + " missing " + String.valueOf(missing));

        Activity context = this.getActivity();
        Intent resultIntent = new Intent(context, MainActivity.class);
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);


        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_launcher);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setContentTitle("DirectAdvertisement")
                .setTicker("DirectAdvertisement")
                .setContentText("Node alert for " + node.id + ":" + node.name + " missing for " + String.valueOf(missing))
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(
                        Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(node.id, mBuilder.build());

    }
}
