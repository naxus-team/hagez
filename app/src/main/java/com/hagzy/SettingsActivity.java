package com.hagzy;
import android.app.Activity;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.hagzy.helpers.DirectionHelper;
import com.hagzy.helpers.LocaleManager;
import com.hagzy.helpers.ThemeManager;


public class SettingsActivity extends AppCompatActivity {


    private void setupInit(Activity activity) {
        LocaleManager.applyLocale(activity);
        ThemeManager.init(activity);
        DirectionHelper.applyDirection(activity, "ar");
        ThemeManager.applySystemBars(activity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupInit(this);



    }
}
