package com.naxus.hagez.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.naxus.hagez.Controller.SlideMenuController;
import com.naxus.hagez.R;
import com.naxus.hagez.adapters.ViewPagerAdapter;
import com.naxus.hagez.fragments.BookingsFragment;
import com.naxus.hagez.fragments.FieldsFragment;
import com.naxus.hagez.fragments.HomeFragment;
import com.naxus.hagez.fragments.ProfileFragment;
import com.naxus.hagez.helpers.ThemeManager;
import com.naxus.hagez.models.TabItem;
import com.naxus.hagez.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class MainLayout {

    private final Context context;
    private FrameLayout root;
    private LinearLayout contentLayout;
    private LinearLayout slideMenu;
    private View overlay;

    private LinearLayout tabLayout;
    private View indicator;
    private ViewPager2 viewPager;
    private TextView title;

    private int indicatorWidth = 0;
    private final List<View> tabButtons = new ArrayList<>();

    private boolean isMenuOpen = false;
    private float menuWidth = 0;

    public MainLayout(Context context) {
        this.context = context;
        buildLayout();
    }

    private void buildLayout() {

        // ────────────── ROOT ──────────────
        root = new FrameLayout(context);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        root.setBackgroundColor(ThemeManager.background());

        int horizontalPadding = (int) (8 * context.getResources().getDisplayMetrics().density);
        int verticalPadding = (int) (4 * context.getResources().getDisplayMetrics().density);

        // ────────────── CONTENT ──────────────
        contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        // ────────────── Top Bar ──────────────
        LinearLayout topBar = new LinearLayout(context);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

        title = new TextView(context);
        title.setText("حجز");
        title.setTextColor(ThemeManager.accent());
        title.setTextSize(24);
        title.setPadding(dp(8), 0, dp(8), 0);
        title.setTypeface(ThemeManager.fontBold());
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        title.setLayoutParams(titleParams);

        LinearLayout menuButton = new LinearLayout(context);
        int btnSize = (int) (48 * context.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(btnSize, btnSize);
        menuButton.setLayoutParams(btnParams);
        menuButton.setGravity(Gravity.CENTER);

        ImageView icon = new ImageView(context);
        int iconSize = (int) (24 * context.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        icon.setLayoutParams(iconParams);
        icon.setImageResource(R.drawable.bars_3);
        icon.setColorFilter(ThemeManager.textPrimary());
        menuButton.addView(icon);

        topBar.addView(title);
        topBar.addView(menuButton);
        contentLayout.addView(topBar);

        // ────────────── Tabs ──────────────
        tabLayout = new LinearLayout(context);
        tabLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tabFrame = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        int margin = (int) (8 * context.getResources().getDisplayMetrics().density);
        tabFrame.setMargins(margin, 0, margin, 0);

        int buttonHeight = (int) (48 * context.getResources().getDisplayMetrics().density); // 48dp
        tabLayout.setLayoutParams(tabFrame);
        tabLayout.setPadding(dp(4), dp(4) , dp(4), dp(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#f5f5f5"));
        bg.setCornerRadius(dp(12));
        tabLayout.setClipToOutline(true);
        tabLayout.setBackground(bg);

        List<TabItem> tabs = new ArrayList<>();
        tabs.add(new TabItem("الحجوزات", null));
        tabs.add(new TabItem("الملاعب", null));
        tabs.add(new TabItem("المسابقات", null));


        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;

            LinearLayout tabButton = new LinearLayout(context);
            tabButton.setOrientation(LinearLayout.VERTICAL);
            tabButton.setGravity(Gravity.CENTER);

            tabButton.setLayoutParams(new LinearLayout.LayoutParams(0, buttonHeight, 1f));
            // نص الزر
            TextView text = new TextView(context);
            text.setText(tabs.get(i).title);
            text.setTextColor(ThemeManager.textPrimary());
            text.setTypeface(ThemeManager.fontSemiBold());
            text.setTextSize(14);
            text.setGravity(Gravity.CENTER);
            tabButton.addView(text);

            // عند الضغط على الزر
            tabButton.setOnClickListener(v -> {
                viewPager.setCurrentItem(index, true);

                for (int j = 0; j < tabButtons.size(); j++) {
                    LinearLayout tab = (LinearLayout) tabButtons.get(j);
                    // تغيير الخلفية حسب الزر المحدد
                    GradientDrawable tabBG = new GradientDrawable();
                    if (j == index) {
                        tabBG.setColor(Color.parseColor("#ffffff")); // زر مختار أبيض
                    } else {
                        tabBG.setColor(Color.parseColor("#f5f5f5")); // باقي الأزرار لون افتراضي
                    }
                    tabBG.setCornerRadius(dp(8));
                    tab.setBackground(tabBG);

                    TextView txt = (TextView) tab.getChildAt(0); // أول عنصر داخل كل tabButton هو النص
                    txt.setTypeface(j == index ? ThemeManager.fontBold() : ThemeManager.fontSemiBold());

                    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                    }
                }
            });

            tabButtons.add(tabButton);
            tabLayout.addView(tabButton);
        }

        contentLayout.addView(tabLayout);

        // ────────────── ViewPager ──────────────
        viewPager = new ViewPager2(context);
        LinearLayout.LayoutParams vpParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        viewPager.setLayoutParams(vpParams);
        contentLayout.addView(viewPager);

        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new BookingsFragment());
        fragments.add(new FieldsFragment());
        fragments.add(new HomeFragment());

        ViewPagerAdapter adapter = new ViewPagerAdapter(((androidx.fragment.app.FragmentActivity) context), fragments);
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                for (int j = 0; j < tabButtons.size(); j++) {
                    LinearLayout tab = (LinearLayout) tabButtons.get(j);

                    // تغيير خلفية الزر حسب الصفحة الحالية
                    GradientDrawable tabBG = new GradientDrawable();
                    tabBG.setCornerRadius(dp(8));
                    if (j == position) {
                        tabBG.setColor(Color.parseColor("#ffffff")); // زر مختار أبيض
                    } else {
                        tabBG.setColor(Color.parseColor("#f5f5f5")); // باقي الأزرار لون افتراضي
                    }
                    tab.setBackground(tabBG);

                    // تغيير نوع الخط للنص داخل كل زر
                    TextView txt = (TextView) tab.getChildAt(0);
                    txt.setTypeface(j == position ? ThemeManager.fontBold() : ThemeManager.fontSemiBold());

                    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                    }
                }
            }
        });

        root.addView(contentLayout);

        SlideMenuController menu = new SlideMenuController(context, root, contentLayout);


        LinearLayout button = new LinearLayout(context);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER); // توسيط العناصر أفقياً وعمودياً
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
        ));

        ImageView profileImage = new ImageView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(40), dp(40));
        profileImage.setLayoutParams(params);
        profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        button.addView(profileImage, 0);
        menu.addMenuItem(button);

        TextView username = new TextView(context);
        username.setTextColor(ThemeManager.textPrimary());
        username.setTextSize(16);
        username.setTypeface(ThemeManager.fontSemiBold());
        button.addView(username);
        SessionManager session = new SessionManager(context);
        if (session.isLoggedIn()) {
            String photoUrl = session.getPhoto();
            String name = session.getName();

            username.setText(name.isEmpty() ? "مستخدم" : name);

            if (!photoUrl.isEmpty()) {
                Glide.with(context)
                        .load(photoUrl)
                        .circleCrop()
                        .into(profileImage);
            } else {
            }
        } else {
            username.setText("مستخدم");
        }

// ────────────── إضافة الزر للقائمة الجانبية ──────────────

        // ────────────── Animations ──────────────
        menuButton.setOnClickListener(v -> menu.toggle());

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            contentLayout.setPadding(0, top, 0, bottom);
            return insets;
        });


    }

    private int dp(int dpValue) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * density + 0.5f);
    }

    // تحويل من pixels إلى dp (لو احتجت)
    private int pxToDp(int pxValue) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / density + 0.5f);
    }

    public FrameLayout getView() {
        return root;
    }
}
