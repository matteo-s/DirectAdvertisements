/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.app;

import java.util.Comparator;

public class NodeView implements Comparable<NodeView> {
    public int id;
    public String name;
    public String address;
    public int clock;
    public int color;
    public boolean hide;
    public boolean alert;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeView nodeView = (NodeView) o;

        return id == nodeView.id;

    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int compareTo(NodeView o) {
        return this.id - o.id;
    }

    public static final Comparator<NodeView> ASCENDING_COMPARATOR = new Comparator<NodeView>() {
        // Overriding the compare method to sort the age
        public int compare(NodeView n, NodeView n1) {
            return n.id - n1.id;
        }
    };
}
