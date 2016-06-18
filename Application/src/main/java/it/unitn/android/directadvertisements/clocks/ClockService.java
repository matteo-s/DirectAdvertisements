/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.clocks;

public interface ClockService {

//        public final static long INTERVAL = 10 * 1000; // 10 seconds
    public final static long INTERVAL = 30 * 1000; // 30 seconds


    /*
    * Clock
    * use short for -32768 to 32767
    * could also use char as unsigned 16bit, from 0 to 65535
     */
    public short get();

    public short increment();

    public void set(short c);

    public void sync(short c);

    public void reset();

    public void start();

    public void stop();

}
