package com.hagzy.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.View;

import java.util.Locale;

public class DirectionHelper {

    public static void applyDirection(Activity activity, String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        activity.getResources().updateConfiguration(config, activity.getResources().getDisplayMetrics());

        if (langCode.equals("ar_AR")) {
            setRTL(activity);
        } else {
            setLTR(activity);
        }
    }

    public static void setRTL(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        decorView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }

    public static void setLTR(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        decorView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
    }

    public static Context wrapContext(Context context, String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        return context.createConfigurationContext(config);
    }
}
