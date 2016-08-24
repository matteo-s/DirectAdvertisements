/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.log.local;

import android.content.Context;
import android.util.Log;

import net.gotev.uploadservice.BinaryUploadRequest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import it.unitn.android.directadvertisements.app.ServiceConnector;
import it.unitn.android.directadvertisements.clocks.ClockService;
import it.unitn.android.directadvertisements.clocks.ClockServiceFactory;
import it.unitn.android.directadvertisements.log.LogService;

public class LocalLogService implements LogService {

    private Context mContext;
    private ServiceConnector mService = null;

    private Map<String, File> _files;
    private DateFormat _dateFormat;
    private DateFormat _logFormat;

    private File _baseDir;

    public LocalLogService(Context context, ServiceConnector serviceConnector) {
        this.mContext = context;
        this.mService = serviceConnector;

        _files = new HashMap();
        _dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        _logFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        _baseDir = context.getFilesDir();

        //bind
        mService.bindService();
    }

    public void destroy() {

        // unbind
        mService.unbindService();
    }

/*
* Helpers
 */

    private File getFile(String target) {
        if (!_files.containsKey(target)) {

            File logFile = new File(_baseDir, target + ".log");

            if (logFile.exists()) {
                _files.put(target, logFile);
            } else {
                try {
                    logFile.createNewFile();

                    _files.put(target, logFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }


        return _files.get(target);
    }


    private void write(File file, String msg) {
        try {
            Date now = new Date();
            short c = 0;
            //TODO: fetch current clock
//            ClockService clock = ClockServiceFactory.getService();
//            if (clock != null) {
//                c = clock.get();
//            }

            //prepare line
//            String line = _dateFormat.format(now) + " \t " + String.valueOf(c) + " \t " + msg;
            String line = _dateFormat.format(now) + " \t " + msg;

            //open with append
            FileOutputStream fos = new FileOutputStream(file, true);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
            writer.write(line);
            writer.newLine();
            writer.flush();

            writer.close();
            fos.close();

        } catch (Exception ex) {
            //ignore
        }
    }


    /*
    * Interface
     */

    @Override
    public void debug(String component, String msg) {
        //use single log file
        msg = "debug." + component + " \t " + msg;
        write(getFile("app"), msg);
    }

    @Override
    public void error(String component, String msg) {
        //use single log file
        msg = "error." + component + " \t " + msg;
        write(getFile("app"), msg);
    }

    @Override
    public void info(String component, String msg) {
        //use single log file
        msg = "info." + component + " \t " + msg;
        write(getFile("app"), msg);
    }


    @Override
    public void clear() {
        try {
            getFile("app").delete();

            //remove from map
            _files.remove("app");
        } catch (Exception ex) {
            //ignore
        }
    }

    @Override
    public String path(String component) {
        //use single log file
        File file = getFile("app");
        return file.getAbsolutePath();
    }


//    @Override
//    public void debug(String component, String msg) {
//        write(getFile("debug." + component), msg);
//    }
//
//    @Override
//    public void error(String component, String msg) {
//        write(getFile("error." + component), msg);
//    }
//
//    @Override
//    public void info(String component, String msg) {
//        write(getFile("info." + component), msg);
//    }

    public String rotate(String component) {
        Date now = new Date();

        String path = path(component) + "." + _logFormat.format(now);
        File rotateFile = new File(path);
        try {
            getFile(component);
            //remove from map and rename
            _files.remove(component).renameTo(rotateFile);

            return rotateFile.getAbsolutePath();
        } catch (Exception ex) {
            return null;
        }

    }


    public void upload(String id, String address, String username, String password) {

        //rotate current log and upload old one
        String path = rotate("app");
        if (path != null) {
            File file = new File(path);
            String fileName = id + "-" + file.getName();
            Log.v("LogService", "upload file " + fileName + " to " + address);

            try {
                final String uploadId = new BinaryUploadRequest(this.mContext, address)
                        .addHeader("file-name", fileName)
                        .setBasicAuth(username, password)
                        .setFileToUpload(path)
                        .setMaxRetries(2)
                        .setDelegate(null)
                        .startUpload();
            } catch (MalformedURLException e) {

            } catch (FileNotFoundException e) {

            }
        }
    }


}
