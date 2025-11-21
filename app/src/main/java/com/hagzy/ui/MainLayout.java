package com.hagzy.ui;

import static com.hagzy.helpers.TranslationManager.t;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.hagzy.Controller.SlideMenuController;
import com.hagzy.InboxActivity;
import com.hagzy.MainActivity;
import com.hagzy.R;
import com.hagzy.adapters.ViewPagerAdapter;
import com.hagzy.fragments.FieldsFragment;
import com.hagzy.fragments.MyBookingsFragment;
import com.hagzy.fragments.TournamentFragment;
import com.hagzy.helpers.ThemeManager;
import com.hagzy.models.TabItem;

import java.util.ArrayList;
import java.util.Arrays;
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

    public class MenuItemModel {
        public int iconRes;
        public String title;

        public MenuItemModel(int iconRes, String title) {
            this.iconRes = iconRes;
            this.title = title;
        }
    }

    public static class MenuSection {
        String title;
        List<MenuItemModel> items;

        public MenuSection(String title, List<MenuItemModel> items) {
            this.title = title;
            this.items = items;
        }
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

        LinearLayout listButton = new LinearLayout(context);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        listButton.setLayoutParams(btnParams);
        listButton.setGravity(Gravity.CENTER);

        LinearLayout searchBtn = new LinearLayout(context);
        LinearLayout.LayoutParams searchBtnParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        searchBtn.setLayoutParams(searchBtnParams);
        searchBtn.setGravity(Gravity.CENTER);

        ImageView searchIcon = new ImageView(context);
        LinearLayout.LayoutParams searchIconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        searchIcon.setLayoutParams(searchIconParams);
        searchIcon.setImageResource(R.drawable.magnifying_glass);
        searchIcon.setColorFilter(Color.parseColor("#4B463D"));
        searchBtn.addView(searchIcon);

        LinearLayout ntfButton = new LinearLayout(context);
        LinearLayout.LayoutParams ntfbtnParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        ntfButton.setLayoutParams(ntfbtnParams);
        ntfButton.setGravity(Gravity.CENTER);



        ImageView ntficon = new ImageView(context);
        LinearLayout.LayoutParams ntfIconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        ntficon.setLayoutParams(ntfIconParams);
        ntficon.setImageResource(R.drawable.inbox);
        ntficon.setColorFilter(Color.parseColor("#4B463D"));
        ntfButton.addView(ntficon);

        LinearLayout menuButton = new LinearLayout(context);
        LinearLayout.LayoutParams btnsParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        menuButton.setLayoutParams(btnsParams);
        menuButton.setGravity(Gravity.CENTER);

        ImageView icon = new ImageView(context);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        icon.setLayoutParams(iconParams);
        icon.setImageResource(R.drawable.bars_3);
        icon.setColorFilter(Color.parseColor("#4B463D"));
        menuButton.addView(icon);

        topBar.addView(title);
        listButton.addView(searchBtn);
        listButton.addView(ntfButton);
        listButton.addView(menuButton);
        topBar.addView(listButton);
        contentLayout.addView(topBar);

        // ────────────── Tabs ──────────────
        tabLayout = new LinearLayout(context);
        tabLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabLayout.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tabFrame = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tabLayout.setLayoutParams(tabFrame);


        // ────────────── Tabs Container ──────────────
        FrameLayout tabContainer = new FrameLayout(context);
        tabContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(40)
        ));
        tabContainer.setPadding(dp(12), 0, dp(12), 0);

// ────────────── Tabs Layout ──────────────
        tabLayout = new LinearLayout(context);
        tabLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabLayout.setGravity(Gravity.CENTER);
        tabLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

// ────────────── Moving Background ──────────────
        View movingBg = new View(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F2EDE8"));
        bg.setCornerRadius(dp(16));
        movingBg.setBackground(bg);

// هنا LayoutParams داخل FrameLayout عشان absolute
        FrameLayout.LayoutParams bgParams = new FrameLayout.LayoutParams(
                0, // width هيظبط بعد ما التابات تتبنى
                dp(40)
        );
        movingBg.setLayoutParams(bgParams);

// ضيف الخلفية أولًا عشان تكون تحت التابات
        tabContainer.addView(movingBg);
        tabContainer.addView(tabLayout);


        List<TabItem> tabs = new ArrayList<>();
        tabs.add(new TabItem(t("bookings"), null));
        tabs.add(new TabItem("الملاعب", null));
        tabs.add(new TabItem("المسابقات", null));


        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;

            LinearLayout tabButton = new LinearLayout(context);
            tabButton.setOrientation(LinearLayout.VERTICAL);
            tabButton.setGravity(Gravity.CENTER);

            tabButton.setLayoutParams(new LinearLayout.LayoutParams(0, dp(40), 1f));
            // نص الزر
            TextView text = new TextView(context);
            text.setText(tabs.get(i).title);
            text.setTextColor(Color.parseColor("#4B463D"));
            text.setTypeface(ThemeManager.fontBold());
            text.setTranslationY(-dpf(1.5f));
            text.setTextSize(14);
            text.setGravity(Gravity.CENTER);
            tabButton.addView(text);

            // عند الضغط على الزر
            tabButton.setOnClickListener(v -> {
                viewPager.setCurrentItem(index, true);

                for (int j = 0; j < tabButtons.size(); j++) {
                    viewPager.setCurrentItem(index, true);

                    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                    }
                }
            });

            tabButtons.add(tabButton);
            tabLayout.addView(tabButton);
        }

        contentLayout.addView(tabContainer);

        // ────────────── ViewPager ──────────────
        viewPager = new ViewPager2(context);
        LinearLayout.LayoutParams vpParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        viewPager.setLayoutParams(vpParams);
        contentLayout.addView(viewPager);

        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new MyBookingsFragment());
        fragments.add(new FieldsFragment());
        fragments.add(new TournamentFragment());

        ViewPagerAdapter adapter = new ViewPagerAdapter(((androidx.fragment.app.FragmentActivity) context), fragments);
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                for (int j = 0; j < tabButtons.size(); j++) {

                    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                    }
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int offsetPx) {
                float offset = (position + positionOffset) * movingBg.getWidth();
                movingBg.setTranslationX(-offset);
            }
        });

        root.addView(contentLayout);

        SlideMenuController menu = new SlideMenuController(context, root, contentLayout);

        LinearLayout menuLayout = new LinearLayout(context);
        menuLayout.setPadding(dp(16), 0, dp(16), 0);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setGravity(Gravity.CENTER);
        menu.addMenuItem(menuLayout);

        List<MenuSection> sections = Arrays.asList(

                new MenuSection("الأصول", Arrays.asList(
                        new MenuItemModel(R.drawable.wallet, "رصيد")
                )),

                new MenuSection("الدعم الفني & الإعدادات", Arrays.asList(
                        new MenuItemModel(R.drawable.adjustments_horizontal, "الدعم الفني")
                )),

                new MenuSection("", Arrays.asList(
                        new MenuItemModel(R.drawable.cog_8, t("settings.title"))
                ))
        );
        int runningIndex = 0;

// بناء الواجهة
        for (MenuSection section : sections) {

            boolean hasTitle = section.title != null && !section.title.trim().isEmpty();

            // ----- العنوان -----
            if (hasTitle) {
                TextView sectionTitle = new TextView(context);
                sectionTitle.setText(section.title);
                sectionTitle.setTextSize(12);
                sectionTitle.setTypeface(ThemeManager.fontBold());
                sectionTitle.setTextColor(Color.parseColor("#999999"));
                sectionTitle.setPadding(dp(8), dp(8), dp(8), 0);
                menuLayout.addView(sectionTitle);
            }
            // ----- عناصر القسم -----
            for (int i = 0; i < section.items.size(); i++) {

                MenuItemModel item = section.items.get(i);

                final int index = runningIndex;
                runningIndex++;

                LinearLayout button = new LinearLayout(context);
                button.setOrientation(LinearLayout.HORIZONTAL);
                button.setGravity(Gravity.CENTER_VERTICAL);
                button.setPadding(dp(8), 0, dp(8), 0);

                // عشان chevron يروح على الآخر
                button.setWeightSum(1);

                LinearLayout.LayoutParams bParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(58)
                );
                button.setLayoutParams(bParams);


                // Icon
                ImageView ic = new ImageView(context);
                LinearLayout.LayoutParams icParams = new LinearLayout.LayoutParams(dp(20), dp(20));
                icParams.setMarginEnd(dp(12));
                ic.setLayoutParams(icParams);
                ic.setImageResource(item.iconRes);
                ic.setColorFilter(Color.parseColor("#000000"));


                // Title
                TextView title = new TextView(context);
                title.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
                ));
                title.setTypeface(ThemeManager.fontBold());
                title.setTextColor(Color.parseColor("#000000"));
                title.setTranslationY(-dpf(1.5f));
                title.setText(item.title);
                title.setTextSize(14);


                // ----- Chevron -----
                ImageView chevron = new ImageView(context);
                LinearLayout.LayoutParams chevronParams = new LinearLayout.LayoutParams(dp(16), dp(16));
                chevron.setLayoutParams(chevronParams);
                chevron.setImageResource(R.drawable.chevron_right);
                chevron.setColorFilter(Color.parseColor("#999999"));


                button.addView(ic);
                button.addView(title);
                button.addView(chevron);

                menuLayout.addView(button);


                // onClick
                button.setOnClickListener(v -> {
                    try {
                        MainActivity activity = (MainActivity) context;

                        switch (index) {
                           case 0:
                               activity.openPage("wallet");
                               break;
                           case 1:
                               // TODO
                               break;
                           case 2:
                               activity.openPage("settings");
                               break;
                           default:
                               // TODO
                               break;
                       }
                        v.postDelayed(() -> {
                            menu.toggle();
                        }, 200);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // ----- فاصل -----
            if (hasTitle) {
                View divider = new View(context);
                LinearLayout.LayoutParams indParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                ) {{
                    setMargins(dp(8), 0, dp(8), 0);
                }};
                divider.setLayoutParams(indParams);
                divider.setBackgroundColor(Color.parseColor("#20000000"));
                menuLayout.addView(divider);
            }
        }

        tabLayout.post(() -> {
            int tabWidth = tabLayout.getWidth() / tabs.size();

            ViewGroup.LayoutParams lp = movingBg.getLayoutParams();
            lp.width = tabWidth;
            movingBg.setLayoutParams(lp);
        });


//        ImageView profileImage = new ImageView(context);
//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(40), dp(40));
//        profileImage.setLayoutParams(params);
//        profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
//        button.addView(profileImage, 0);
//        menu.addMenuItem(button);
//
//        TextView username = new TextView(context);
//        username.setTextColor(ThemeManager.textPrimary());
//        username.setTextSize(16);
//        username.setTypeface(ThemeManager.fontSemiBold());
//        button.addView(username);
//        SessionManager session = new SessionManager(context);
//        if (session.isLoggedIn()) {
//            String photoUrl = session.getPhoto();
//            String name = session.getName();
//            Log.d("PHOTOS", "buildLayout: "+photoUrl);
//
//            username.setText(name.isEmpty() ? "مستخدم" : name);
//
//            if (!photoUrl.isEmpty()) {
//                Glide.with(context)
//                        .load(photoUrl)
//                        .circleCrop()
//                        .into(profileImage);
//            } else {
//            }
//        } else {
//            username.setText("مستخدم");
//        }

// ────────────── إضافة الزر للقائمة الجانبية ──────────────

        // ────────────── Animations ──────────────
        menuButton.setOnClickListener(v -> menu.toggle());
        ntfButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, InboxActivity.class);
            context.startActivity(intent);
        });


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

    float dpf(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()
        );
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
