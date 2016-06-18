/**
 * Created by mat on 2016
 */

package it.unitn.android.directadvertisements.network;

import java.io.Serializable;

public class NetworkNode implements Serializable {
    //
    //ids are local to each node - 0 means undefined
    public int id;
    //address is global, network dependant
    public String address;
    //name is broadcasted, can be updated
    public String name;
    //clock is broadcasted
    public short clock;

    public NetworkNode() {
        id = 0;
        address = "";
        name = "";
        clock = 0;
    }

    public NetworkNode(int i) {
        id = i;
        address = "";
        name = "";
        clock = 0;
    }


    //    /*
//    * equals = same  address
//     */
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//
//        NetworkNode that = (NetworkNode) o;
//
//        return !(address != null ? !address.equals(that.address) : that.address != null);
//
//    }
//    @Override
//    public int hashCode() {
//        return address != null ? address.hashCode() : 0;
//    }

    /*
    * equals = same id
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkNode that = (NetworkNode) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return (int) id;
    }
/*
     * toString
    */

    @Override
    public String toString() {
        return "NetworkNode{" +
                "address='" + address + '\'' +
                ", name='" + name + '\'' +
                ", clock=" + clock +
                '}';
    }
}
