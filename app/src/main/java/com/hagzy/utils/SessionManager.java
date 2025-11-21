package com.hagzy.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "user_session";
    private static final String KEY_CLIENT_ID = "client_id";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_PHOTO = "user_photo";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    Context context;

    public SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveUser(String id, String name, String email, String photoUrl) {
        editor.putString(KEY_CLIENT_ID, id);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PHOTO, photoUrl);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getName() {
        return prefs.getString(KEY_NAME, "");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getPhoto() {
        return prefs.getString(KEY_PHOTO, "");
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
