/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.log;

public interface LogService {


    public void debug(String component, String msg);

    public void error(String component, String msg);

    public void info(String component, String msg);

    public void clear();

    public String path(String component);

}
