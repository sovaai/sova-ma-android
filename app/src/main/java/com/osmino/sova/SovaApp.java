package com.osmino.sova;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class SovaApp  extends Application {
    private static final String TAG = "SovaApp";

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Sova application created");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String themePref = "off";//prefs.getString("preference_dark_mode", "auto");
        if (themePref != null) {
            setDarkThemeMode(themePref);
        }
    }

    public static boolean setDarkThemeMode(@NonNull String mode) {
        if (mode.equals("auto")) {
            if (Build.VERSION.SDK_INT >= 28) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
            }
        } else if (mode.equals("on")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (mode.equals("off")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            return false;
        }
        return true;
    }
}
