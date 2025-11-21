package com.hagzy.helpers;

import android.graphics.Color;

public class ColorHelper {

    public static final int BACKGROUND = Color.parseColor("#121212");
    public static final int BACKGROUND_SECONDARY = Color.parseColor("#1E1E1E");

    public static final int ACCENT = Color.parseColor("#65E030");
    public static final int ACCENT_VARIANT = Color.parseColor("#4CAF50");

    public static final int TEXT_PRIMARY = Color.parseColor("#FFFFFF");
    public static final int TEXT_SECONDARY = Color.parseColor("#B0B0B0");

    public static final int ERROR = Color.parseColor("#FF3B30");
    public static final int WARNING = Color.parseColor("#FFCC00");
    public static final int SUCCESS = Color.parseColor("#4CD964");

    // 🔄 لو حبيت مستقبلاً تحول Light Mode
    public static int getBackground(boolean darkMode) {
        return darkMode ? BACKGROUND : Color.parseColor("#FFFFFF");
    }

    public static int getTextPrimary(boolean darkMode) {
        return darkMode ? TEXT_PRIMARY : Color.parseColor("#000000");
    }

    public static int getAccent() {
        return ACCENT;
    }
}
