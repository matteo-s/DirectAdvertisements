/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.clocks.local;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import it.unitn.android.directadvertisements.app.MessageKeys;
import it.unitn.android.directadvertisements.clocks.ClockService;

public class LocalClockService implements ClockService {

    private short _c;
    private Messenger mMessenger;
    boolean isBound = false;
    private boolean isActive = false;
    private Timer mTimer = null;


    public LocalClockService(Context context, IBinder binder) {
        Log.v("LocalClockService", "create");

        this._c = 0;

        //bind
        this.mMessenger = new Messenger(binder);
        this.isBound = true;

    }


    @Override
    public short get() {
        return _c;
    }

    @Override
    public short increment() {
        return _c++;
    }

    @Override
    public void set(short c) {
        _c = c;

        //call update if running timer
        if (isActive) {
            update();
        }
    }

    @Override
    public void sync(short c) {
        _c = c;

        //reset timer if active
        if (isActive) {
            stop();
            update();
            start();
        }
    }

    @Override
    public void reset() {
        _c = 0;
    }

    @Override
    public void start() {
        Log.v("LocalClockService", "start");

        // cancel if already existed
        if (mTimer != null) {
            mTimer.cancel();
        }
        // recreate new
        mTimer = new Timer();

        // schedule task
        mTimer.scheduleAtFixedRate(new ClockTimerTask(), 0, ClockService.INTERVAL);
        isActive = true;
    }

    @Override
    public void stop() {
        Log.v("LocalClockService", "stop");

        //stop timer
        if (mTimer != null) {
            mTimer.cancel();
        }
        isActive = false;
    }


    private void update() {
        //notify via msg
        if (isBound && isActive) {
            Message msg = Message.obtain(null, MessageKeys.CLOCK_INCREMENT, 0, 0);

            try {
                mMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClockTimerTask extends TimerTask {

        @Override
        public void run() {
            Log.v("LocalClockService", "timer task");

            increment();
            update();
        }

    }
}
