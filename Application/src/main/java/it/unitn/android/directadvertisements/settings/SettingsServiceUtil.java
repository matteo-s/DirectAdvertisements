/**
 * Created by mat - 2016
 */

package it.unitn.android.directadvertisements.settings;

import android.content.Context;

public class SettingsServiceUtil {
    private static SettingsService _service;


    public static SettingsService getService(Context context) {
        if (_service == null) {
            _service = new SettingsService(context);
        }
        return _service;
    }

    public static SettingsService getService() {
        return _service;
    }
}
