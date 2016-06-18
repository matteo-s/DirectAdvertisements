/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.network.simulator;

import android.content.Context;
import android.os.IBinder;

import it.unitn.android.directadvertisements.network.simulator.local.LocalSimulatorService;

public class SimulatorServiceUtil {
    private static SimulatorService _service;


    public static SimulatorService getService(Context context, IBinder binder) {
        if (_service == null) {
            _service = new LocalSimulatorService(context, binder);
        }
        return _service;
    }
}
