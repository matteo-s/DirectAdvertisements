/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.app;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import java.util.LinkedList;
import java.util.List;

import it.unitn.android.directadvertisements.MainActivity;

public class ProxyFragment extends ListFragment {
    private List<ProxyView> mItems;
    public ProxyViewAdapter viewAdapter;


    public void addItem(String address, String name) {
        if (name == null) {
            name = "";
        }

        Log.v("ProxyFragment", "add item " + address + " " + name);

        ProxyView w = new ProxyView();
        w.address = address;
        w.name = name;
        if (!mItems.contains(w)) {
            mItems.add(w);
        }

        //refresh
        refreshItems();
    }

    public void refreshItems() {
        Log.v("ProxyFragment", "refresh items " + String.valueOf(mItems.size()));

        viewAdapter.clear();

//        viewAdapter.addAll(mItems);
        for (ProxyView w : mItems) {
            viewAdapter.add(w);
        }
        viewAdapter.notifyDataSetChanged();

        Log.v("ProxyFragment", "refreshed count " + String.valueOf(viewAdapter.getCount()));

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v("ProxyFragment", "onCreate super");

        Resources resources = getResources();

        // initialize the items list
        mItems = new LinkedList<>();

        ProxyView w = new ProxyView();

        //use a dedicated list for viewAdapter content
        viewAdapter = new ProxyViewAdapter(getActivity(), new LinkedList<ProxyView>());
        Log.v("ProxyFragment", "onCreate viewAdapter");

        // initialize and set the list adapter
        setListAdapter(viewAdapter);
        Log.v("ProxyFragment", "onCreate setAdapter");

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
        ProxyView item = mItems.get(position);

        ((MainActivity) getActivity()).proxySelect(item);
    }
}
