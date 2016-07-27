/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import it.unitn.android.directadvertisements.app.MainService;
import it.unitn.android.directadvertisements.app.NodesFragment;
import it.unitn.android.directadvertisements.log.LogServiceUtil;
import it.unitn.android.directadvertisements.settings.SettingsService;

public class LogActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        setTitle(R.string.activity_main_title);

        Log.v("LogActivity", "onCreate");


        final Button closeButton = (Button) findViewById(R.id.log_button_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //load main activity
                activityMain();
            }
        });

        final Button clearButton = (Button) findViewById(R.id.log_button_close);
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clearLog();
            }
        });

        displayLog();
    }


    private void displayLog() {
        //read log from file and populate
        File file = new File(this.getFilesDir(), "app.log");
        StringBuilder logText = new StringBuilder();
        try {
            if (file.exists()) {
                //open with append
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;

                //read settings
                while ((line = reader.readLine()) != null) {
                    //split
                    if (!line.isEmpty()) {
                        logText.append(line);
                        logText.append(System.getProperty("line.separator"));
                    }
                }

                reader.close();
            }
        } catch (Exception ex) {
            //ignore
        }

         TextView labelField = (TextView) findViewById(R.id.log_field_label);
        labelField.setText("app.log");


        TextView textField = (TextView) findViewById(R.id.log_text_content);
        textField.setText(logText.toString());


    }

    private void clearLog() {
        LogServiceUtil.getLogger(this).clear();
        //load main activity
        activityMain();
    }

    public void activityMain() {
        Log.v("MainActivity", "activityMain");


        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

    }
}
