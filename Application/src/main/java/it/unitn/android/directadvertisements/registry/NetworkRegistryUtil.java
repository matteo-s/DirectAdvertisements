/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.registry;

import android.content.Context;
import android.os.IBinder;

import java.util.List;

import it.unitn.android.directadvertisements.network.NetworkNode;

public class NetworkRegistryUtil {


    private static NetworkRegistry _registry;


    public static NetworkRegistry getRegistry() {
        if (_registry == null) {
            _registry = new LocalNetworkRegistry();
        }
        return _registry;
    }

}
