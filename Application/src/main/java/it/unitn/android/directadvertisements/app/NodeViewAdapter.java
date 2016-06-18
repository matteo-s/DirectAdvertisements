/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.app;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import it.unitn.android.directadvertisements.R;
import it.unitn.android.directadvertisements.network.NetworkNode;

public class NodeViewAdapter extends ArrayAdapter<NetworkNode> {

    public NodeViewAdapter(Context context, List<NetworkNode> items) {

        super(context, R.layout.listitem_node, items);
        Log.v("NodeViewAdapter", "onCreate");

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        NetworkNode item = getItem(position);

        if (convertView == null) {
            // inflate the GridView item layout
            LayoutInflater inflater = LayoutInflater.from(getContext());
//            convertView = inflater.inflate(R.layout.listitem_node, parent, false);
            convertView = inflater.inflate(R.layout.listitem_node, null, false);


            // initialize the view holder
            viewHolder = new ViewHolder();
            viewHolder.nName = (TextView) convertView.findViewById(R.id.node_name);
            viewHolder.nAddress = (TextView) convertView.findViewById(R.id.node_address);
            viewHolder.nClock = (TextView) convertView.findViewById(R.id.node_clock);
            convertView.setTag(viewHolder);
        } else {
            // recycle the already inflated view
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // update the item view
        viewHolder.nName.setText(item.name);
        viewHolder.nAddress.setText(item.address);
        viewHolder.nClock.setText(String.valueOf(item.clock));

        return convertView;
    }

    /**
     * The view holder design pattern prevents using findViewById()
     * repeatedly in the getView() method of the adapter.
     *
     * @see http://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
     */
    private static class ViewHolder {
        TextView nName;
        TextView nAddress;
        TextView nClock;
    }
}

