package com.hagzy.Controller;

import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SlideMenuController {
    private final Context context;
    private final FrameLayout root;
    private final LinearLayout contentLayout;
    private View overlay;
    private LinearLayout slideMenu;
    private boolean isMenuOpen = false;
    private boolean isRTL = true;
    private final int menuWidthDp = 335;
    private final float contentShiftFactor = 1.0f; // اضبط للتحكم بمدى سحب الـ content

    public SlideMenuController(Context context, FrameLayout root, LinearLayout contentLayout) {
        this.context = context;
        this.root = root;
        this.contentLayout = contentLayout;
        initOverlay();
        initSlideMenu();
    }

    private void initOverlay() {
        overlay = new View(context);
        overlay.setBackgroundColor(0x80000000);
        overlay.setAlpha(0f);
        overlay.setVisibility(View.GONE);
        root.addView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        overlay.setOnClickListener(v -> toggleMenu(false));
    }

    private void initSlideMenu() {
        slideMenu = new LinearLayout(context);
        slideMenu.setOrientation(LinearLayout.VERTICAL);
        slideMenu.setBackgroundColor(Color.parseColor("#FFFFFF"));

        int menuPx = dp(menuWidthDp);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(menuPx, FrameLayout.LayoutParams.MATCH_PARENT);
        params.gravity = isRTL ? Gravity.END : Gravity.START;
        slideMenu.setLayoutParams(params);

        // ضبط Insets (status/navigation)
        root.post(() -> {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(root);
            if (insets != null) {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                slideMenu.setPadding(0, top, 0, bottom);
            }
        });

        // ابدأ خارج الشاشة في جهة الفتح
        slideMenu.setTranslationX(isRTL ? -dp(menuWidthDp) : dp(menuWidthDp));
        root.addView(slideMenu);
        slideMenu.setOnTouchListener((v, e) -> true); // منع اللمس داخل الخلفية أثناء ظهور المنيو
    }

    /**
     * يفتح أو يقفل المنيو بشكل سلس، المنيو و الـ content يتحركوا في نفس اتجاه الفتح.
     * @param open true لفتح، false لغلق
     */
    private void toggleMenu(boolean open) {
        if (slideMenu == null || contentLayout == null) return;

        final float menuPx = dp(menuWidthDp);
        // بداية ونهاية قيمة translationX للـ menu
        float start = slideMenu.getTranslationX();
        float end = open ? 0f : (isRTL ? -menuPx : menuPx);

        // عرض overlay قبل تشغيل الانيميشن
        overlay.setVisibility(View.VISIBLE);

        ValueAnimator anim = ValueAnimator.ofFloat(start, end);
        anim.setDuration(200);
        anim.setInterpolator(new android.view.animation.DecelerateInterpolator());

        anim.addUpdateListener(a -> {
            float value = (float) a.getAnimatedValue(); // current translationX for menu
            float fraction = a.getAnimatedFraction();

            // حرك المنيو مباشرة
            slideMenu.setTranslationX(value);

            // احسب مقدار سحب الـ content:
            // عندما value عند البداية يكون abs(value)=menuPx -> menu مغلق -> shift = 0
            // عندما value == 0 -> menu مفتوح -> shift = menuPx * factor
            float shift = (menuPx - Math.abs(value)) * contentShiftFactor;
            // اجعل الـ content يتحرك في نفس اتجاه فتح المنيو:
            float contentTranslation = (isRTL ? 1f : -1f) * shift;
            contentLayout.setTranslationX(contentTranslation);

            // overlay alpha
            overlay.setAlpha(open ? fraction : 1f - fraction);
        });

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (!open) overlay.setVisibility(View.GONE);
                isMenuOpen = open;
            }
        });

        anim.start();
    }

    // واجهات خارجية
    public void toggle() {
        toggleMenu(!isMenuOpen);
    }

    public void open() { toggleMenu(true); }
    public void close() { toggleMenu(false); }
    public boolean isOpen() { return isMenuOpen; }

    /**
     * اضبط اتجاه الفتح (true تفتح من اليمين، false من اليسار).
     * استدعِها قبل أي فتح/إضافة عناصر.
     */
    public void setDirectionRTL(boolean rtl) {
        this.isRTL = rtl;
        if (slideMenu == null) return;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) slideMenu.getLayoutParams();
        params.gravity = rtl ? Gravity.END : Gravity.START;
        slideMenu.setLayoutParams(params);
        // ضع المنيو أولًا خارج الشاشة في الجهة الصحيحة
        slideMenu.setTranslationX(rtl ? dp(menuWidthDp) : -dp(menuWidthDp));
    }

    public void addMenuItem(View item) {
        slideMenu.addView(item);
    }

    private int dp(int v) {
        return (int) (v * context.getResources().getDisplayMetrics().density);
    }

    float dpf(float value) {
        assert context != null;
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()
        );
    }
}
