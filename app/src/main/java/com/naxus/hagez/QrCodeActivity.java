package com.naxus.hagez;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.naxus.hagez.fragments.QrGenerateFragment;
import com.naxus.hagez.fragments.QrScanFragment;
import com.naxus.hagez.helpers.ThemeManager;



public class QrCodeActivity extends AppCompatActivity {

    private TextView tabGenerate, tabScan;
    private int fragmentContainerId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔹 إنشاء الواجهة العامة
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        // 🔹 الشريط العلوي Tabs
        LinearLayout tabContainer = new LinearLayout(this);
        tabContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabContainer.setGravity(Gravity.CENTER);
        tabContainer.setPadding(dp(16), dp(12), dp(16), dp(12));
        tabContainer.setBackgroundColor(Color.parseColor("#F5F5F5"));

        tabGenerate = createTab("عرض الكود");
        tabScan = createTab("مسح الكود");

        tabContainer.addView(tabGenerate);
        tabContainer.addView(tabScan);

        root.addView(tabContainer);

        // 🔹 الحاوية للفراجمنتات
        LinearLayout fragmentContainer = new LinearLayout(this);
        fragmentContainerId = View.generateViewId();
        fragmentContainer.setId(fragmentContainerId);
        fragmentContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        root.addView(fragmentContainer);

        setContentView(root);

        // 🔹 أول فراجمنت افتراضي
        openFragment(new QrGenerateFragment());
        highlightTab(tabGenerate);

        // 🔹 التعامل مع التبديل
        tabGenerate.setOnClickListener(v -> {
            openFragment(new QrGenerateFragment());
            highlightTab(tabGenerate);
        });

        tabScan.setOnClickListener(v -> {
            openFragment(new QrScanFragment());
            highlightTab(tabScan);
        });
    }

    private TextView createTab(String title) {
        TextView tab = new TextView(this);
        tab.setText(title);
        tab.setTextSize(16);
        tab.setTextColor(Color.parseColor("#65e030"));
        tab.setTypeface(ThemeManager.fontSemiBold());
        tab.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(40), 1f);
        tab.setLayoutParams(params);
        return tab;
    }

    private void highlightTab(TextView activeTab) {
        tabGenerate.setTextColor(Color.parseColor("#777777"));
        tabScan.setTextColor(Color.parseColor("#777777"));
        activeTab.setTextColor(Color.parseColor("#000000"));
    }

    private void openFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(fragmentContainerId, fragment)
                .commit();
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }
}