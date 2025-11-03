package com.naxus.hagez.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.core.content.res.ResourcesCompat;

import com.naxus.hagez.R;

public class ThemeManager {

    private static final String PREF_NAME = "theme_prefs";
    private static final String KEY_IS_DARK = "is_dark";
    private static boolean isDark = false; // الافتراضي: فاتح

    // 🔹 تحميل الحالة من SharedPreferences
    private static Typeface cairoRegular;
    private static Typeface cairoBold;
    private static Typeface cairoMedium;
    private static Typeface cairoLight;
    private static Typeface cairoSemiBold;
    private static Typeface cairoExtraBold;
    private static Typeface cairoBlack;

    // 🔹 تحميل الحالة + الخطوط
    public static void init(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        isDark = prefs.getBoolean(KEY_IS_DARK, false);

        if (cairoRegular == null) {
            cairoRegular = ResourcesCompat.getFont(context, R.font.cairo_regular);
            cairoBold = ResourcesCompat.getFont(context, R.font.cairo_bold);
            cairoMedium = ResourcesCompat.getFont(context, R.font.cairo_medium);
            cairoLight = ResourcesCompat.getFont(context, R.font.cairo_light);
            cairoSemiBold = ResourcesCompat.getFont(context, R.font.cairo_semibold);
            cairoExtraBold = ResourcesCompat.getFont(context, R.font.cairo_extrabold);
            cairoBlack = ResourcesCompat.getFont(context, R.font.cairo_black);
        }
    }

    // 🔹 Fonts Getter
    public static Typeface fontRegular() { return cairoRegular; }
    public static Typeface fontBold() { return cairoBold; }
    public static Typeface fontMedium() { return cairoMedium; }
    public static Typeface fontLight() { return cairoLight; }
    public static Typeface fontSemiBold() { return cairoSemiBold; }
    public static Typeface fontExtraBold() { return cairoExtraBold; }
    public static Typeface fontBlack() { return cairoBlack; }

    // 🔹 تبديل الوضع
    public static void toggleTheme(Context context) {
        isDark = !isDark;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_IS_DARK, isDark).apply();
    }

    // 🔹 ضبط يدوي
    public static void setDarkMode(Context context, boolean dark) {
        isDark = dark;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_IS_DARK, dark).apply();
    }

    // 🎨 تطبيق الثيم على System Bars
    public static void applySystemBars(Activity activity) {
        Window window = activity.getWindow();
        View decor = window.getDecorView();

        // السماح للمحتوى بالتمدد تحت شريط الحالة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }

        if (isDark) {
            // 🌙 الوضع الداكن
            window.setStatusBarColor(background());
            window.setNavigationBarColor(background());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = window.getInsetsController();
                if (controller != null) {
                    controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                }
            } else {
                // إزالة أيقونات داكنة (يعني تصبح فاتحة)
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }

        } else {
            // ☀️ الوضع الفاتح
            // نجعل الـ status bar شفاف فعلاً
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.WHITE);

            // نمد المحتوى تحت شريط الحالة
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = window.getInsetsController();
                if (controller != null) {
                    // أيقونات داكنة في الوضع الفاتح
                    controller.setSystemBarsAppearance(
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    );
                }
            } else {
                // أيقونات داكنة لأندرويد 6–10
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }

            decor.setSystemUiVisibility(flags);
        }
    }

    // 🔹 معرفة الوضع الحالي
    public static boolean isDark() {
        return isDark;
    }

    // 🎨 ألوان موحدة
    public static int background() {
        return isDark ? Color.parseColor("#121212") : Color.parseColor("#FFFFFF");
    }

    public static int backgroundSecondary() {
        return isDark ? Color.parseColor("#1E1E1E") : Color.parseColor("#F5F5F5");
    }

    public static int divider() {
        return isDark ? Color.parseColor("#2A2A2A") : Color.parseColor("#E0E0E0");
    }

    public static int textPrimary() {
        return isDark ? Color.parseColor("#FFFFFF") : Color.parseColor("#000000");
    }

    public static int black() {
        return Color.parseColor("#000000");
    }

    public static int textSecondary() {
        return isDark ? Color.parseColor("#B0B0B0") : Color.parseColor("#444444");
    }

    public static int accent() {
        return Color.parseColor("#65E030"); // الأساسي
    }

    public static int accentLight() {
        return Color.parseColor("#B9F48F"); // فاتح
    }

    public static int accentDark() {
        return Color.parseColor("#4BB821"); // غامق
    }

    public static int accentDarkest() {
        return Color.parseColor("#2F7C14"); // أغمق جدًا
    }

    public static int accentLightest() {
        return Color.parseColor("#E9FBE0"); // أفتح جدًا (خلفية)
    }


    public static int accentVariant() {
        return Color.parseColor("#4CAF50");
    }

    public static int error() {
        return Color.parseColor("#FF3B30");
    }

    public static int success() {
        return Color.parseColor("#4CD964");
    }
}
