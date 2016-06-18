/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unitn.android.directadvertisements.network.NetworkNode;

public class LocalNetworkRegistry implements NetworkRegistry {


    private Map<Integer, NetworkNode> nodes;

    public LocalNetworkRegistry() {
        nodes = new HashMap<>();
    }


    @Override
    public void clear() {
        nodes = new HashMap<>();
    }

    @Override
    public int countNodes() {
        return nodes.size();
    }

    @Override
    public ArrayList<NetworkNode> getNodes() {
        return new ArrayList(nodes.values());
    }

    @Override
    public boolean hasNode(String address) {
        boolean found = false;
        Iterator<NetworkNode> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            NetworkNode n = iter.next();
            if (n.address.equals(address)) {
                found = true;
                break;
            }
        }

        return found;
    }

    @Override
    public boolean hasNode(int id) {
        return nodes.containsKey(id);
    }

    @Override
    public NetworkNode getNode(String address) {
        Iterator<NetworkNode> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            NetworkNode n = iter.next();
            if (n.address.equals(address)) {
                return n;
            }
        }

        return null;
    }

    @Override
    public NetworkNode getNode(int id) {
        if (nodes.containsKey(id)) {
            return nodes.get(id);
        } else {
            return null;
        }
    }

    @Override
    public void addNode(NetworkNode n) {
        if (!nodes.containsKey(n.id)) {
            //add
            nodes.put(n.id, n);

        } else {
            NetworkNode node = nodes.get(n.id);
            node.address = n.address;
            node.name = n.name;
            node.clock = n.clock;

            nodes.put(node.id, node);

        }
    }

    @Override
    public NetworkNode updateNode(NetworkNode n) {
        if (!nodes.containsKey(n.id)) {
            //add
            nodes.put(n.id, n);

            return n;
        } else {
            NetworkNode node = nodes.get(n.id);
            node.address = n.address;
            node.name = n.name;
            node.clock = n.clock;

            nodes.put(node.id, node);

            return node;
        }
    }

    @Override
    public void removeNode(NetworkNode n) {
        if (nodes.containsKey(n.id)) {
            NetworkNode node = nodes.get(n.id);
            nodes.remove(node);
        }
    }

    @Override
    public void removeNode(String address) {
        Iterator<NetworkNode> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            NetworkNode n = iter.next();
            if (n.address.equals(address)) {
                nodes.remove(n);
            }
        }

    }

    @Override
    public void removeNode(int id) {
        if (nodes.containsKey(id)) {
            NetworkNode node = nodes.get(id);
            nodes.remove(node);
        }
    }
}
