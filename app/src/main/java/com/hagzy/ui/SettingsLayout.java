package com.hagzy.ui;

import static com.hagzy.helpers.TranslationManager.t;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hagzy.BuildConfig;
import com.hagzy.MainActivity;
import com.hagzy.R;
import com.hagzy.RootActivity;
import com.hagzy.helpers.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class SettingsLayout {

    private final Context context;
    private FrameLayout root;
    private LinearLayout contentLayout;
    private ScrollView scrollView;
    private ViewPager2 viewPager;
    private LinearLayout headerContainer;
    private TextView headerTitle;
    private ImageView backButton;

    // Navigation state
    private int currentPage = 0;
    private static final int PAGE_MAIN = 0;
    private static final int PAGE_LANGUAGE = 1;
    private static final int PAGE_NOTIFICATIONS = 2;
    private static final int PAGE_PRIVACY = 3;
    private static final int PAGE_TERMS = 4;

    public SettingsLayout(Context context) {
        this.context = context;
        buildLayout();
    }

    private void buildLayout() {
        // Root container
        root = new FrameLayout(context);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        root.setBackgroundColor(Color.WHITE);

        // Content layout
        contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Handle insets
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            contentLayout.setPadding(0, top, 0, bottom);
            return insets;
        });

        buildHeader();
        buildViewPager();

        root.addView(contentLayout);
    }

    private void buildHeader() {
        headerContainer = new LinearLayout(context);
        headerContainer.setOrientation(LinearLayout.HORIZONTAL);
        headerContainer.setGravity(Gravity.CENTER_VERTICAL);
        headerContainer.setPadding(dp(16), dp(12), dp(16), dp(12));
        headerContainer.setBackgroundColor(Color.WHITE);

        // Back button
        backButton = new ImageView(context);
        backButton.setImageResource(R.drawable.chevron_right);
        backButton.setRotation(180);
        backButton.setColorFilter(Color.parseColor("#4B463D"), PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        backParams.setMarginEnd(dp(12));
        backButton.setLayoutParams(backParams);
        backButton.setOnClickListener(v -> handleBackPress());

        // Title
        headerTitle = new TextView(context);
        headerTitle.setText(t("settings.title"));
        headerTitle.setTextSize(18);
        headerTitle.setTypeface(ThemeManager.fontBold());
        headerTitle.setTextColor(Color.parseColor("#4B463D"));
        headerTitle.setTranslationY(-dpf(1.5f));

        headerContainer.addView(backButton);
        headerContainer.addView(headerTitle);

        // Add shadow
        GradientDrawable headerBg = new GradientDrawable();
        headerBg.setColor(Color.WHITE);
        headerContainer.setBackground(headerBg);
        headerContainer.setElevation(dp(4));

        contentLayout.addView(headerContainer);
    }

    private void buildViewPager() {
        viewPager = new ViewPager2(context);
        LinearLayout.LayoutParams vpParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        viewPager.setLayoutParams(vpParams);
        viewPager.setUserInputEnabled(false); // Disable swipe
        viewPager.setOffscreenPageLimit(1);

        // Create pages
        List<View> pages = new ArrayList<>();
        pages.add(createMainPage());
        pages.add(createLanguagePage());
        pages.add(createNotificationsPage());
        pages.add(createPrivacyPage());
        pages.add(createTermsPage());

        // Simple adapter
        viewPager.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<ViewHolder>() {
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                FrameLayout container = new FrameLayout(context);
                container.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
                return new ViewHolder(container);
            }

            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                ((FrameLayout) holder.itemView).removeAllViews();
                ((FrameLayout) holder.itemView).addView(pages.get(position));
            }

            @Override
            public int getItemCount() {
                return pages.size();
            }
        });

        contentLayout.addView(viewPager);
    }

    // ViewHolder class
    static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    private void handleBackPress() {
        if (currentPage == PAGE_MAIN) {
            // Exit settings
            if (context instanceof MainActivity) {
                ((MainActivity) context).onBackPressed();
            }
        } else {
            // Go back to main page
            navigateToPage(PAGE_MAIN, t("settings.title"));
        }
    }

    private void navigateToPage(int page, String title) {
        currentPage = page;
        headerTitle.setText(title);
        viewPager.setCurrentItem(page, true);
    }

    private View createMainPage() {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout scrollContent = new LinearLayout(context);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        scrollContent.setPadding(dp(16), dp(16), dp(16), dp(16));

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String name = currentUser.getDisplayName();     // اسم المستخدم من Google
            String email = currentUser.getEmail();         // البريد الإلكتروني
            Uri photoUri = currentUser.getPhotoUrl();      // رابط الصورة (Uri)

            String photoUrl = (photoUri != null) ? photoUri.toString() : null;
            scrollContent.addView(createHeroSection(name, email, photoUrl));
        }


        // Account section
        scrollContent.addView(createSectionTitle(t("settings.account")));
        scrollContent.addView(createSettingCard(
                t("settings.edit_profile"),
                t("settings.edit_profile_desc"),
                R.drawable.cog_8,
                () -> Toast.makeText(context, "Edit Profile", Toast.LENGTH_SHORT).show()
        ));

        // Preferences section
        scrollContent.addView(createSectionTitle(t("settings.preferences")));
        scrollContent.addView(createSettingCard(
                t("field.language"),
                t("settings.language_desc"),
                R.drawable.cog_8,
                () -> navigateToPage(PAGE_LANGUAGE, t("field.language"))
        ));
        scrollContent.addView(createSettingCard(
                t("settings.notifications"),
                t("settings.notifications_desc"),
                R.drawable.cog_8,
                () -> navigateToPage(PAGE_NOTIFICATIONS, t("settings.notifications"))
        ));

        // About section
        scrollContent.addView(createSectionTitle(t("settings.about")));
        scrollContent.addView(createSettingCard(
                t("settings.privacy_policy"),
                t("settings.privacy_policy_desc"),
                R.drawable.cog_8,
                () -> navigateToPage(PAGE_PRIVACY, t("settings.privacy_policy"))
        ));
        scrollContent.addView(createSettingCard(
                t("settings.terms"),
                t("settings.terms_desc"),
                R.drawable.cog_8,
                () -> navigateToPage(PAGE_TERMS, t("settings.terms"))
        ));

        // Logout section
        scrollContent.addView(createSectionTitle(t("settings.account_actions")));
        scrollContent.addView(createLogoutCard());
        String versionName = BuildConfig.VERSION_NAME;    // 1.0.0.19
        int versionCode = BuildConfig.VERSION_CODE;       // 19
        String buildType = BuildConfig.BUILD_TYPE;        // debug / release
        String variantName = BuildConfig.APP_MODE;    // debug / release
        // Version info
        TextView versionText = new TextView(context);
        String appInfo = "v" + versionName +
                " • " + buildType;

        versionText.setText(appInfo);
        versionText.setTextSize(14);
        versionText.setTypeface(ThemeManager.fontSemiBold());
        versionText.setTextColor(Color.parseColor("#804B463D"));
        versionText.setGravity(Gravity.CENTER);
        versionText.setPadding(dp(16), dp(24), dp(16), dp(16));
        versionText.setTranslationY(-dpf(1f));
        scrollContent.addView(versionText);

        scrollView.addView(scrollContent);
        return scrollView;
    }

    private View createLanguagePage() {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        content.addView(createLanguageOption("العربية", "ar", true));
        content.addView(createLanguageOption("English", "en", false));
        content.addView(createLanguageOption("Français", "fr", false));

        scrollView.addView(content);
        return scrollView;
    }

    private View createNotificationsPage() {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        content.addView(createSectionTitle("إشعارات الحجز"));
        content.addView(createToggleCard("تأكيد الحجز", "احصل على إشعار عند تأكيد حجزك", true));
        content.addView(createToggleCard("تذكير الحجز", "تذكير قبل موعد الحجز بساعة", true));

        content.addView(createSectionTitle("إشعارات التطبيق"));
        content.addView(createToggleCard("العروض الخاصة", "احصل على إشعارات العروض والخصومات", false));
        content.addView(createToggleCard("التحديثات", "أخبار وتحديثات التطبيق", true));

        scrollView.addView(content);
        return scrollView;
    }

    private View createPrivacyPage() {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView privacyText = new TextView(context);
        privacyText.setText("سياسة الخصوصية\n\n" +
                "نحن نحترم خصوصيتك ونلتزم بحماية بياناتك الشخصية...\n\n" +
                "جمع المعلومات\n" +
                "نقوم بجمع المعلومات التي تقدمها عند التسجيل...");
        privacyText.setTextSize(14);
        privacyText.setTypeface(ThemeManager.fontSemiBold());
        privacyText.setTextColor(Color.parseColor("#4B463D"));
        privacyText.setLineSpacing(dp(4), 1.0f);

        content.addView(privacyText);
        scrollView.addView(content);
        return scrollView;
    }

    private View createTermsPage() {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView termsText = new TextView(context);
        termsText.setText("الشروط والأحكام\n\n" +
                "باستخدامك لهذا التطبيق، فإنك توافق على الشروط التالية...\n\n" +
                "استخدام التطبيق\n" +
                "يحق لك استخدام التطبيق للأغراض الشخصية فقط...");
        termsText.setTextSize(14);
        termsText.setTypeface(ThemeManager.fontSemiBold());
        termsText.setTextColor(Color.parseColor("#4B463D"));
        termsText.setLineSpacing(dp(4), 1.0f);

        content.addView(termsText);
        scrollView.addView(content);
        return scrollView;
    }

    private CardView createLanguageOption(String language, String code, boolean selected) {
        CardView card = new CardView(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(16));
        card.setCardElevation(0);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        if (selected) {
            bg.setColor(Color.parseColor("#E8F5E9"));
            bg.setStroke(dp(2), Color.parseColor("#4CAF50"));
        } else {
            bg.setStroke(dp(2), Color.parseColor("#EFEDE9"));
            bg.setColor(Color.WHITE);
        }
        card.setBackground(bg);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView languageText = new TextView(context);
        languageText.setText(language);
        languageText.setTextSize(16);
        languageText.setTypeface(ThemeManager.fontBold());
        languageText.setTextColor(selected ? Color.parseColor("#4CAF50") : Color.parseColor("#4B463D"));
        languageText.setTranslationY(-dpf(1.5f));
        languageText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (selected) {
            ImageView checkIcon = new ImageView(context);
            checkIcon.setImageResource(R.drawable.check_badge);
            checkIcon.setColorFilter(Color.parseColor("#4CAF50"));
            checkIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(24), dp(24)));
            content.addView(checkIcon);
        }

        content.addView(languageText, 0);
        card.addView(content);

        card.setOnClickListener(v -> {
            Toast.makeText(context, "Selected: " + language, Toast.LENGTH_SHORT).show();
        });

        return card;
    }

    private CardView createToggleCard(String title, String description, boolean enabled) {
        CardView card = new CardView(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(16));
        card.setCardElevation(0);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(2), Color.parseColor("#EFEDE9"));
        bg.setColor(Color.WHITE);
        card.setBackground(bg);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView titleText = new TextView(context);
        titleText.setText(title);
        titleText.setTextSize(16);
        titleText.setTypeface(ThemeManager.fontBold());
        titleText.setTextColor(Color.parseColor("#4B463D"));
        titleText.setTranslationY(-dpf(1.5f));

        TextView descText = new TextView(context);
        descText.setText(description);
        descText.setTextSize(12);
        descText.setTypeface(ThemeManager.fontSemiBold());
        descText.setTextColor(Color.parseColor("#804B463D"));
        descText.setTranslationY(-dpf(1f));
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        descParams.topMargin = dp(2);
        descText.setLayoutParams(descParams);

        textColumn.addView(titleText);
        textColumn.addView(descText);

        // Simple toggle indicator
        View toggle = new View(context);
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(dp(48), dp(28));
        toggle.setLayoutParams(toggleParams);
        GradientDrawable toggleBg = new GradientDrawable();
        toggleBg.setCornerRadius(dp(14));
        toggleBg.setColor(enabled ? Color.parseColor("#4CAF50") : Color.parseColor("#E0E0E0"));
        toggle.setBackground(toggleBg);

        content.addView(textColumn);
        content.addView(toggle);

        card.setOnClickListener(v -> {
            Toast.makeText(context, title + " toggled", Toast.LENGTH_SHORT).show();
        });

        card.addView(content);
        return card;
    }

    private CardView createHeroSection(String name, String email, String photoUrl) {
        CardView card = new CardView(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(24);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(24));
        card.setCardElevation(0);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(24));
        bg.setColor(Color.parseColor("#EFEDE9"));
        card.setBackground(bg);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(24), dp(24), dp(24), dp(24));

        // Profile image
        ImageView profileImg = new ImageView(context);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(dp(80), dp(80));
        imgParams.bottomMargin = dp(12);
        profileImg.setLayoutParams(imgParams);
        GradientDrawable imgBg = new GradientDrawable();
        imgBg.setShape(GradientDrawable.OVAL);
        imgBg.setColor(Color.WHITE);
        profileImg.setBackground(imgBg);
        profileImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        profileImg.setClipToOutline(true);

        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(context).load(photoUrl).into(profileImg); // تحميل الصورة من URL
        } else {
            profileImg.setImageResource(R.drawable.check_badge); // صورة افتراضية
        }

        // User name
        TextView userName = new TextView(context);
        userName.setText(name != null ? name : "—");
        userName.setTextSize(24);
        userName.setTypeface(ThemeManager.fontBold());
        userName.setTextColor(Color.parseColor("#4B463D"));
        userName.setTranslationY(-dpf(2f));

        // User email
        TextView userEmail = new TextView(context);
        userEmail.setText(email != null ? email : "—");
        userEmail.setTextSize(14);
        userEmail.setTypeface(ThemeManager.fontSemiBold());
        userEmail.setTextColor(Color.parseColor("#804B463D"));
        userEmail.setTranslationY(-dpf(1.5f));

        LinearLayout contentHero = new LinearLayout(context);
        contentHero.setOrientation(LinearLayout.VERTICAL);
        contentHero.setGravity(Gravity.CENTER);
        contentHero.setPadding(dp(24), dp(24), dp(24), dp(24));

        contentHero.addView(profileImg);
        contentHero.addView(userName);
        contentHero.addView(userEmail);
        card.addView(contentHero);

        return card;
    }

    private TextView createSectionTitle(String title) {
        TextView section = new TextView(context);
        section.setText(title);
        section.setTextSize(14);
        section.setTypeface(ThemeManager.fontBold());
        section.setTextColor(Color.parseColor("#804B463D"));
        section.setTranslationY(-dpf(1.5f));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(8);
        params.bottomMargin = dp(12);
        section.setLayoutParams(params);
        return section;
    }

    private CardView createSettingCard(String title, String description, int iconRes, Runnable onClick) {
        CardView card = new CardView(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(16));
        card.setCardElevation(0);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(2), Color.parseColor("#EFEDE9"));
        bg.setColor(Color.WHITE);
        card.setBackground(bg);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Icon
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#4B463D"));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        iconParams.setMarginEnd(dp(12));
        icon.setLayoutParams(iconParams);

        // Text column
        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView titleText = new TextView(context);
        titleText.setText(title);
        titleText.setTextSize(16);
        titleText.setTypeface(ThemeManager.fontBold());
        titleText.setTextColor(Color.parseColor("#4B463D"));
        titleText.setTranslationY(-dpf(1.5f));

        TextView descText = new TextView(context);
        descText.setText(description);
        descText.setTextSize(12);
        descText.setTypeface(ThemeManager.fontSemiBold());
        descText.setTextColor(Color.parseColor("#804B463D"));
        descText.setTranslationY(-dpf(1f));
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        descParams.topMargin = dp(2);
        descText.setLayoutParams(descParams);

        textColumn.addView(titleText);
        textColumn.addView(descText);

        // Arrow
        ImageView arrow = new ImageView(context);
        arrow.setImageResource(R.drawable.chevron_left);
        arrow.setRotation(180);
        arrow.setColorFilter(Color.parseColor("#804B463D"));
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(20), dp(20));
        arrow.setLayoutParams(arrowParams);

        content.addView(icon);
        content.addView(textColumn);
        content.addView(arrow);

        card.setOnClickListener(v -> {
            if (onClick != null) onClick.run();
        });

        card.addView(content);
        return card;
    }

    private CardView createLogoutCard() {
        CardView card = new CardView(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(16));
        card.setCardElevation(0);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(Color.parseColor("#FFEBEE"));
        card.setBackground(bg);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Icon
        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.cog_8);
        icon.setColorFilter(Color.parseColor("#F44336"));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        iconParams.setMarginEnd(dp(12));
        icon.setLayoutParams(iconParams);

        // Text
        TextView titleText = new TextView(context);
        titleText.setText(t("field.logout"));
        titleText.setTextSize(16);
        titleText.setTypeface(ThemeManager.fontBold());
        titleText.setTextColor(Color.parseColor("#F44336"));
        titleText.setTranslationY(-dpf(1.5f));
        titleText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Arrow
        ImageView arrow = new ImageView(context);
        arrow.setImageResource(R.drawable.chevron_right);
        arrow.setRotation(180);
        arrow.setColorFilter(Color.parseColor("#F44336"));
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(20), dp(20));
        arrow.setLayoutParams(arrowParams);

        content.addView(icon);
        content.addView(titleText);
        content.addView(arrow);

        card.setOnClickListener(v -> {
            // 1. تسجيل الخروج من Firebase
            FirebaseAuth.getInstance().signOut();

            // 2. تسجيل الخروج من Google
            GoogleSignIn.getClient(
                    context,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut().addOnCompleteListener(task -> {
                // بعد تسجيل الخروج، اعادة توجيه المستخدم لشاشة تسجيل الدخول
                Intent intent = new Intent(context, RootActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);
                if (context instanceof Activity) {
                    ((Activity) context).finish(); // اغلق الـ Activity الحالي
                }
            });
        });


        card.addView(content);
        return card;
    }

    private int dp(int dpValue) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * density + 0.5f);
    }

    private float dpf(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()
        );
    }

    public FrameLayout getView() {
        return root;
    }
}