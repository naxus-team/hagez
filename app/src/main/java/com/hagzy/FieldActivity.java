package com.hagzy;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.fragment.app.Fragment;

import com.hagzy.fragments.BookingsFragment;
import com.hagzy.fragments.FieldMainFragment;
import com.hagzy.helpers.DirectionHelper;
import com.hagzy.helpers.LocaleManager;
import com.hagzy.helpers.ThemeManager;
import com.hagzy.helpers.TranslationManager;

public class FieldActivity extends AppCompatActivity {

    private FrameLayout container;
    private ViewPager2 viewPager;
    private FragmentStateAdapter adapter;
    private String currentFieldId;

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed(){
        if(viewPager.getCurrentItem()==1){
            viewPager.setCurrentItem(0,true);
        }else{
            super.onBackPressed();
        }
    }

    private void setupInit() {
        LocaleManager.setLocale(this, "ar_AR");
        LocaleManager.applyLocale(this);
        ThemeManager.init(this);
        DirectionHelper.applyDirection(this, LocaleManager.getSavedLanguage(this));
        ThemeManager.applySystemBars(this);
        TranslationManager.load(this, LocaleManager.getSavedLanguage(this));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupInit();
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        container = new FrameLayout(this);
        container.setId(View.generateViewId());
        setContentView(container);

        setupViewPager();

        // مثال: فتح صفحة الحجوزات مباشرة
/*
        goToBookings("FIELD_ID_123");
*/

    }

    private void setupViewPager() {
        viewPager = new ViewPager2(this);
        viewPager.setId(View.generateViewId());
        viewPager.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        addContentView(viewPager, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        adapter = new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0: return new FieldMainFragment();
                    case 1: return BookingsFragment.newInstance(currentFieldId != null ? currentFieldId : "");
                    default: return new Fragment();
                }
            }

            @Override
            public int getItemCount() { return 2; }
        };

        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false); // منع السحب بالإصبع
    }

    // الانتقال إلى صفحة الحجوزات وتمرير الـ fieldId
    public void goToBookings(String fieldId) {
        currentFieldId = fieldId;
        viewPager.setCurrentItem(1, true);

        // تمرير البيانات للـ Fragment الموجود فعليًا
        getSupportFragmentManager().executePendingTransactions();
        Fragment f = getSupportFragmentManager().findFragmentByTag("f1"); // الصفحة الثانية
        if (f instanceof BookingsFragment) {
            ((BookingsFragment) f).setFieldId(fieldId);
            ((BookingsFragment) f).loadData();
        }
    }

    // التحكم بالصفحة من أي Fragment
    public void setCurrentPage(int index) {
        if (viewPager != null) {
            viewPager.setCurrentItem(index, true);
        }
    }

    public ViewPager2 getViewPager() {
        return viewPager;
    }
}
