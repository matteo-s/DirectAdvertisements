/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.log.local;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import it.unitn.android.directadvertisements.clocks.ClockService;
import it.unitn.android.directadvertisements.clocks.ClockServiceUtil;
import it.unitn.android.directadvertisements.log.LogService;

public class LocalLogService implements LogService {

    private Map<String, File> _files;
    private DateFormat _dateFormat;
    private File _baseDir;

    public LocalLogService(Context context) {
        _files = new HashMap();
        _dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        _baseDir = context.getFilesDir();
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
            ClockService clock = ClockServiceUtil.getService();
            if (clock != null) {
                c = clock.get();
            }

            //prepare line
            String line = _dateFormat.format(now) + " \t " + String.valueOf(c) + " \t " + msg;

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
        msg = "\t debug." + component + " \t" + msg;
        write(getFile("app"), msg);
    }

    @Override
    public void error(String component, String msg) {
        //use single log file
        msg = "\t error." + component + " \t" + msg;
        write(getFile("app"), msg);
    }

    @Override
    public void info(String component, String msg) {
        //use single log file
        msg = "\t info." + component + " \t" + msg;
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
}
