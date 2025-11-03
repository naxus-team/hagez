package com.naxus.hagez.Controller;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.naxus.hagez.helpers.ThemeManager;

public class SlideMenuController {

    private final Context context;
    private final FrameLayout root;
    private final LinearLayout contentLayout;
    private View overlay;
    private LinearLayout slideMenu;
    private int menuWidth;
    private boolean isMenuOpen = false;

    public SlideMenuController(Context context, FrameLayout root, LinearLayout contentLayout) {
        this.context = context;
        this.root = root;
        this.contentLayout = contentLayout;

        initOverlay();
        initSlideMenu();
    }

    // ────────────── إنشاء الـ Overlay ──────────────
    private void initOverlay() {
        overlay = new View(context);
        overlay.setBackgroundColor(0x80000000); // نصف شفاف
        overlay.setAlpha(0f);
        overlay.setVisibility(View.GONE);
        root.addView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        overlay.setOnClickListener(v -> toggleMenu(false));
    }

    // ────────────── إنشاء القائمة الجانبية ──────────────
    private void initSlideMenu() {
        slideMenu = new LinearLayout(context);
        slideMenu.setOrientation(LinearLayout.VERTICAL);
        slideMenu.setBackgroundColor(ThemeManager.background());

        menuWidth = (int) (335 * context.getResources().getDisplayMetrics().density);

        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(menuWidth, FrameLayout.LayoutParams.MATCH_PARENT);
        menuParams.gravity = Gravity.START; // بدل END، تصبح على اليسار
        slideMenu.setLayoutParams(menuParams);

        root.post(() -> {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(root);
            if (insets != null) {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

                slideMenu.setPadding(0, top, 0, bottom);
            }
        });


        slideMenu.setTranslationX(menuWidth + 35); // تبدأ خارج الشاشة
        root.addView(slideMenu);

        slideMenu.setOnTouchListener((v, event) -> true); // منع التفاعل مع الخلفية
    }

    // ────────────── فتح / غلق القائمة ──────────────
    private void toggleMenu(boolean open) {
        float start = slideMenu.getTranslationX();
        float end = open ? 0 : menuWidth + FrameLayout.LayoutParams.MATCH_PARENT;

        overlay.setVisibility(View.VISIBLE);

        ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(150);
        animator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());

        animator.addUpdateListener(a -> {
            float value = (float) a.getAnimatedValue();
            float progress = 1 - Math.abs(value / menuWidth);

            slideMenu.setTranslationX(value);

            contentLayout.setTranslationX(-menuWidth * progress * 1f);

            overlay.setAlpha(progress);
        });

        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (!open) {
                    overlay.setVisibility(View.GONE);
                    contentLayout.setTranslationX(0);
                }
                isMenuOpen = open;
            }
        });

        animator.start();
    }

    public void toggle() {
        toggleMenu(!isMenuOpen);
    }

    public boolean isOpen() {
        return isMenuOpen;
    }

    // إضافة عنصر للقائمة
    public void addMenuItem(View item) {
        slideMenu.addView(item);
    }
}
