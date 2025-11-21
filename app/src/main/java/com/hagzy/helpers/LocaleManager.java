package com.hagzy.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.view.View;

import java.util.Locale;

public class LocaleManager {

    private static final String PREF_NAME = "locale_prefs";
    private static final String KEY_LANGUAGE = "app_language";

    // 🔹 استدعاء عام لتطبيق اللغة والاتجاه
    public static void applyLocale(Activity activity) {
        String lang = getSavedLanguage(activity);
        if (lang == null) lang = "ar_AR"; // اللغة الافتراضية

        setLocale(activity, lang);
    }

    // 🔹 تعيين اللغة وحفظها
    public static void setLocale(Activity activity, String langCode) {
        saveLanguage(activity, langCode);

        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        activity.getResources().updateConfiguration(config, activity.getResources().getDisplayMetrics());

        // تفعيل الاتجاه
        View decorView = activity.getWindow().getDecorView();
        if (langCode.equals("ar_AR")) {
            decorView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        } else {
            decorView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
    }


    // 🔹 التفاف الـ Context (لـ Fragments أو Application)
    public static Context wrapContext(Context context) {
        String lang = getSavedLanguage(context);
        if (lang == null) lang = "ar_AR";

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        return context.createConfigurationContext(config);
    }

    // 🧠 تخزين واسترجاع اللغة
    private static void saveLanguage(Context context, String langCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, langCode).apply();
    }

    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, null);
    }
}
