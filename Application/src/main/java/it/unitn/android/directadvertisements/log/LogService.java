/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.log;

public interface LogService {

    /*
    * upload static configuration
     */
    public static String UPLOAD_ADDRESS = "http://host46.tn.ymir.eu:3000/upload/binary-ba";
    public static String UPLOAD_USERNAME = "logger";
    public static String UPLOAD_PASSWORD = "directAdvertisements";


    public void destroy();

    public void debug(String component, String msg);

    public void error(String component, String msg);

    public void info(String component, String msg);

    public void clear();

    public String path(String component);

    public void upload(String id, String address, String username, String password);
}
