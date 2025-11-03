package com.naxus.hagez;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.naxus.hagez.helpers.DirectionHelper;
import com.naxus.hagez.helpers.LocaleManager;
import com.naxus.hagez.helpers.ThemeManager;
import com.naxus.hagez.ui.MainLayout;

public class MainActivity extends AppCompatActivity {

    private MainLayout mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocaleManager.applyLocale(this);
        ThemeManager.init(this);
        DirectionHelper.applyDirection(this, "ar");

        mainLayout = new MainLayout(this);
        setContentView(mainLayout.getView());
        ThemeManager.applySystemBars(this);

    }

    public MainLayout getMainLayout() {
        return mainLayout;
    }
}
