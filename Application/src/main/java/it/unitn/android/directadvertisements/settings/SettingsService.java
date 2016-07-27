/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.settings;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class SettingsService {

    private File _file;
    private File _baseDir;
    private Map<String, String> _settings;

    public SettingsService(Context context) {
        _file = null;
        _baseDir = context.getFilesDir();
        _settings = new HashMap<>();

        //read from file
        read();
    }

    /*
    * Settings
     */

    public String getSetting(String key, String fallback) {
        if (_settings.containsKey(key)) {
            return _settings.get(key);
        } else {
            return fallback;
        }
    }

    public void setSetting(String key, String value) {
        _settings.put(key, value);
    }

    public void clearSetting(String key) {
        if (_settings.containsKey(key)) {
            _settings.remove(key);
        }
    }


    /*
    * Handlers
     */

    public void readSettings() {
        Log.v("SettingsService", "read settings from file");

        read();
    }

    public void writeSettings() {
        Log.v("SettingsService", "write settings to file");

        write();
    }




/*
* Helpers
 */

    private File getFile() {
        if (_file == null) {

            _file = new File(_baseDir, "app.conf");

            if (!_file.exists()) {

                try {
                    _file.createNewFile();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }


        return _file;
    }


    private void read() {
        try {
            //open with append
            BufferedReader reader = new BufferedReader(new FileReader(getFile()));
            String line;

            //read settings
            while ((line = reader.readLine()) != null) {
                //split
                if (!line.isEmpty() && line.contains("=")) {
                    String[] s = line.split("=");
                    _settings.put(s[0], s[1]);
                }
            }

            reader.close();

            //log
            Log.v("SettingsService", "read settings " + String.valueOf(_settings.keySet()));


        } catch (Exception ex) {
            //ignore
        }
    }


    private void write() {
        try {
            //open with append
            FileOutputStream fos = new FileOutputStream(getFile());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));

            Log.v("SettingsService", "write settings " + String.valueOf(_settings.keySet()));


            //write settings
            for (String key : _settings.keySet()) {
                String value = _settings.get(key);
                String line = key + "=" + value;


                writer.write(line);
                writer.newLine();
            }

            writer.flush();

            writer.close();
            fos.close();

        } catch (Exception ex) {
            //ignore
        }
    }

}
