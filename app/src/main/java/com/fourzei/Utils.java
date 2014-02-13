package com.fourzei;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class Utils {
    public static String getData(Context ctx, String field) {
        try {
            SharedPreferences prefs = ctx.getSharedPreferences("com.fourzei", Context.MODE_PRIVATE);
            return prefs.getString("com.fourzei." + field.toLowerCase(), null);
        } catch (Exception e) {
            return null;
        }
    }

    public static void setData(Context ctx, String field, String value) {
        try {
            SharedPreferences prefs = ctx.getSharedPreferences("com.fourzei", Context.MODE_PRIVATE);
            prefs.edit().putString("com.fourzei." + field.toLowerCase(), value).commit();
        } catch (Exception e) {
            Log.d("FOURZEI PREFS", "Error setting pref: " + e.getMessage());
        }
    }

    public static long getLastRequest(Context ctx) {
        try {
            SharedPreferences prefs = ctx.getSharedPreferences("com.fourzei", Context.MODE_PRIVATE);
            return prefs.getLong("com.fourzei.lastrequest", -1);
        } catch (Exception e) {
            return -1;
        }
    }

    public static void setLastRequest(Context ctx, long value) {
        try {
            SharedPreferences prefs = ctx.getSharedPreferences("com.fourzei", Context.MODE_PRIVATE);
            prefs.edit().putLong("com.fourzei.lastrequest", value).commit();
        } catch (Exception e) {
            Log.d("FOURZEI PREFS", "Error setting pref: " + e.getMessage());
        }
    }

    public static int getRandomIndex(int length) {
        return Math.min((int) Math.round(Math.random() * length), length - 1);
    }
}
