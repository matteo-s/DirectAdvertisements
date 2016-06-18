/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.registry;

import java.util.ArrayList;
import java.util.List;

import it.unitn.android.directadvertisements.network.NetworkNode;

public interface NetworkRegistry {

    public void clear();

    public int countNodes();
    public ArrayList<NetworkNode> getNodes();


    public boolean hasNode(String address);

    public boolean hasNode(int id);

    public NetworkNode getNode(String address);

    public NetworkNode getNode(int id);

    public void addNode(NetworkNode n);

    public NetworkNode updateNode(NetworkNode n);

    public void removeNode(NetworkNode n);

    public void removeNode(String address);

    public void removeNode(int id);

}
