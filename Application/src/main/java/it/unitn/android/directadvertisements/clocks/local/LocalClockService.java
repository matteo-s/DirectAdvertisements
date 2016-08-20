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
import java.util.UUID;

import it.unitn.android.directadvertisements.app.MessageKeys;
import it.unitn.android.directadvertisements.app.ServiceConnector;
import it.unitn.android.directadvertisements.clocks.ClockService;

public class LocalClockService implements ClockService {

    private short _c;
    private boolean isActive = false;
    private Timer mTimer = null;
    private ClockTimerTask mTask = null;
    private ServiceConnector mService = null;

    public LocalClockService(Context context, ServiceConnector serviceConnector) {
        Log.v("LocalClockService", "create");

        this._c = 0;

        this.mService = serviceConnector;

    }

    public void destroy() {
        stopTimer();
        //unbind
        mService.unbindService();
    }


    @Override
    public short get() {
        return _c;
    }

    @Override
    public short increment() {
        _c++;
        //check boundaries
        if (_c > (2 * Byte.MAX_VALUE)) {
            //reset to 1
            _c = 1;
        }
        Log.v("LocalClockService", "increment to " + String.valueOf(_c));

        return _c;
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
        Log.v("LocalClockService", "sync to " + String.valueOf(c));
        _c = c;

        //reset timer if active
        if (isActive) {
//            //set skip flag to ignore current running task - demanded to stop
//            mTask.skip = true;

            stopTimer();
            startTimer();


            //disable update on sync to avoid immediate retransmission?
            update();
        }
    }

    @Override
    public void reset() {
        _c = 0;
    }

    @Override
    public void start() {
        Log.v("LocalClockService", "start");

        //bind
        mService.bindService();

        isActive = true;
        startTimer();


    }

    @Override
    public void stop() {
        Log.v("LocalClockService", "stop");

        isActive = false;
        stopTimer();

        //unbind
        mService.unbindService();
    }

    private void startTimer() {
        Log.v("LocalClockService", "startTimer");


        // cancel if already existed
        if (mTimer != null) {
            Log.v("LocalClockService", "start cancel timer");
            mTimer.cancel();
        }
        // recreate new
        mTask = new ClockTimerTask();
        mTimer = new Timer();

        // schedule task
        Log.v("LocalClockService", "schedule task " + mTask.uuid + " timer " + String.valueOf(ClockService.INTERVAL));
        //use interval for first run delay AND for subsequent periods
        mTimer.scheduleAtFixedRate(mTask, ClockService.INTERVAL, ClockService.INTERVAL);
    }

    private void stopTimer() {
        Log.v("LocalClockService", "stop");

        //stop timer
        if (mTask != null) {
            Log.v("LocalClockService", "stop cancel task " + mTask.uuid);
            //set skip flag to ignore current running task
            mTask.skip = true;
        }

        if (mTimer != null) {
            Log.v("LocalClockService", "stop cancel timer");
            mTimer.cancel();
        }
    }


    private void update() {
        //notify via msg
        if (isActive) {
            Log.v("LocalClockService", "send update for " + String.valueOf(_c));
            mService.sendMessage(MessageKeys.CLOCK_INCREMENT, null);
        }
    }

    private class ClockTimerTask extends TimerTask {
        public boolean skip = false;
        public String uuid;

        public ClockTimerTask() {
            uuid = UUID.randomUUID().toString();
        }

        @Override
        public void run() {
            Log.v("LocalClockService", "timer task " + uuid);
            if (!skip) {
                increment();
                update();
            } else {
                //clear skip
                skip = false;
            }

        }

    }
}
