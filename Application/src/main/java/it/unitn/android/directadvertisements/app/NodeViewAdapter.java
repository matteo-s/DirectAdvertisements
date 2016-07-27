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

public class NodeViewAdapter extends ArrayAdapter<NodeView> {

    public NodeViewAdapter(Context context, List<NodeView> items) {

        super(context, R.layout.listitem_node, items);
        Log.v("NodeViewAdapter", "onCreate");

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        NodeView item = getItem(position);

        if (convertView == null) {
            // inflate the GridView item layout
            LayoutInflater inflater = LayoutInflater.from(getContext());
//            convertView = inflater.inflate(R.layout.listitem_node, parent, false);
            convertView = inflater.inflate(R.layout.listitem_node, null, false);


            // initialize the view holder
            viewHolder = new ViewHolder();
            viewHolder.nId = (TextView) convertView.findViewById(R.id.node_id);
            viewHolder.nName = (TextView) convertView.findViewById(R.id.node_name);
            viewHolder.nAddress = (TextView) convertView.findViewById(R.id.node_address);
            viewHolder.nClock = (TextView) convertView.findViewById(R.id.node_clock);
            convertView.setTag(viewHolder);
        } else {
            // recycle the already inflated view
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // update the item view
        viewHolder.nId.setText('#'+String.valueOf(item.id));
        viewHolder.nName.setText(item.name);
        viewHolder.nAddress.setText(item.address);
        viewHolder.nClock.setText(String.valueOf(item.clock));

        //set color
//        convertView.setBackgroundColor(item.color);
        convertView.setBackgroundResource(item.color);

        return convertView;
    }

    /**
     * The view holder design pattern prevents using findViewById()
     * repeatedly in the getView() method of the adapter.
     *
     * @see http://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
     */
    private static class ViewHolder {
        TextView nId;
        TextView nName;
        TextView nAddress;
        TextView nClock;
    }
}

