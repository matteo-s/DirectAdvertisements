/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.ListFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.R.color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import it.unitn.android.directadvertisements.R;
import it.unitn.android.directadvertisements.network.NetworkNode;
import it.unitn.android.directadvertisements.settings.SettingsService;
import it.unitn.android.directadvertisements.settings.SettingsServiceUtil;

public class NodesFragment extends ListFragment {
    private List<NetworkNode> mItems;
    private List<NodeView> mViews;
    private short mClock;
    public NodeViewAdapter viewAdapter;

    public void updateItems() {
        updateItems(mClock, mItems);
    }
    public void updateItems(short clock, List<NetworkNode> items) {
        Log.v("NodesFragment", "updateItems clock " + String.valueOf(clock) + " items " + String.valueOf(items.size()));

        //get settings
        SettingsService settings = SettingsServiceUtil.getService();

        mClock = clock;
        mItems = items;
        //update
        viewAdapter.clear();

        //build display nodeView
        mViews = new LinkedList<>();
        for (NetworkNode node : mItems) {
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


            if (w.clock >= mClock) {
                w.color = color.holo_green_light;
            } else if (w.clock == (mClock - 1)) {
                w.color = color.holo_orange_light;
            } else if (w.clock < (mClock - 1)) {
                w.color = color.holo_red_light;
            }

            Log.v("NodesFragment", "updateItems item " + String.valueOf(w.id) + " clock " + String.valueOf(w.clock) + " color " + String.valueOf(w.color));

            //look in settings if hidden
            boolean hide = false;
            if (settings != null) {
                hide = Boolean.parseBoolean(settings.getSetting("node." + Integer.toString(node.id) + ".hide", "false"));
            }
            if (!hide) {
                mViews.add(w);
            }

        }

        //sort
        Collections.sort(mViews, NodeView.ASCENDING_COMPARATOR);

        viewAdapter.addAll(mViews);


        viewAdapter.notifyDataSetChanged();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v("NodesFragment", "onCreate super");


        // initialize the items list
        mItems = new ArrayList<NetworkNode>();
        Resources resources = getResources();

        mViews = new LinkedList<>();

        viewAdapter = new NodeViewAdapter(getActivity(), mViews);
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
                NodeView item = mViews.get(position);
                NetworkNode node = null;
                for (NetworkNode n : mItems) {
                    if (n.id == item.id) {
                        node = n;
                        break;
                    }
                }

                if (node != null) {
                    editName(node);
                }


                return true;
            }
        });
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // retrieve theListView item
        NodeView item = mViews.get(position);

        String msg = "node " + String.valueOf(item.id) + " clock " + String.valueOf(item.clock);

        // do something
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }


    private void editName(final NetworkNode node) {
        Context context = getActivity();

        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.activity_prompt, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptsView);

        final EditText editName = (EditText) promptsView.findViewById(R.id.prompt_edit_field);
        editName.setText(node.name);

        final TextView editLabel = (TextView) promptsView.findViewById(R.id.prompt_label_field);
        editLabel.setText("Rename " + String.valueOf(node.id));

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String name = editName.getText().toString();
                                //update
                                node.name = name;
                                //add to settings
                                SettingsService settings = SettingsServiceUtil.getService();
                                if (settings != null) {
                                    if(name.isEmpty()) {
                                        settings.clearSetting("node." + Integer.toString(node.id) + ".name");
                                    } else {
                                        settings.setSetting("node." + Integer.toString(node.id) + ".name", node.name);
                                    }
                                    settings.writeSettings();
                                }

                                //force update
                                updateItems(mClock, mItems);
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
}
