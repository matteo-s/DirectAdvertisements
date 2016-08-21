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

public class ProxyViewAdapter extends ArrayAdapter<ProxyView> {

    public ProxyViewAdapter(Context context, List<ProxyView> items) {

        super(context, R.layout.listitem_proxy, items);
        Log.v("ProxyViewAdapter", "onCreate");

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        ProxyView item = getItem(position);

        if (convertView == null) {
            // inflate the GridView item layout
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.listitem_proxy, null, false);


            // initialize the view holder
            viewHolder = new ViewHolder();
            viewHolder.nName = (TextView) convertView.findViewById(R.id.proxy_name);
            viewHolder.nAddress = (TextView) convertView.findViewById(R.id.proxy_address);
            convertView.setTag(viewHolder);
        } else {
            // recycle the already inflated view
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // update the item view
        viewHolder.nName.setText(item.name);
        viewHolder.nAddress.setText(item.address);

        //set color
//        convertView.setBackgroundColor(item.color);
        convertView.setBackgroundResource(android.R.color.holo_blue_light);

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
    }
}
