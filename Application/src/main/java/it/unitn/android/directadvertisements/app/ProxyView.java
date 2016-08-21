/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.app;

import java.util.Comparator;

public class ProxyView implements Comparable<ProxyView> {
    public String name;
    public String address;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProxyView w = (ProxyView) o;

        return address.equals(w.address);

    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public int compareTo(ProxyView o) {
        return this.address.compareTo(o.address);
    }

    public static final Comparator<ProxyView> ASCENDING_COMPARATOR = new Comparator<ProxyView>() {
        // Overriding the compare method to sort the age
        public int compare(ProxyView n, ProxyView n1) {
            return n.address.compareTo(n1.address);
        }
    };
}
