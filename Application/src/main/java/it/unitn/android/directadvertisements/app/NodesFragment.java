/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.app;

import android.support.v4.app.ListFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import it.unitn.android.directadvertisements.network.NetworkNode;

public class NodesFragment extends ListFragment {
    private List<NetworkNode> mItems;        // ListView items list
    public NodeViewAdapter viewAdapter;

    public void updateItems(List<NetworkNode> items) {
        Log.v("NodesFragment", "updateItems " + String.valueOf(items.size()));
        mItems = items;
        //update
        viewAdapter.clear();
        viewAdapter.addAll(mItems);
        viewAdapter.notifyDataSetChanged();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v("NodesFragment", "onCreate super");


        // initialize the items list
        mItems = new ArrayList<NetworkNode>();
        Resources resources = getResources();

        viewAdapter = new NodeViewAdapter(getActivity(), mItems);
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
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // retrieve theListView item
//        NetworkNode item = mItems.get(position);

//        // do something
//        Toast.makeText(getActivity(), item.title, Toast.LENGTH_SHORT).show();
    }
}
