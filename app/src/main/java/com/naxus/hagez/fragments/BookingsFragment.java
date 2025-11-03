package com.naxus.hagez.fragments;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.naxus.hagez.helpers.ThemeManager;

public class BookingsFragment extends Fragment {

    private final String[] titles = {"الملغاة", "السابقة", "الموصى بها"};
    private LinearLayout root;

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(dp(8), dp(8), dp(8), dp(8));

        root.addView(createCurrentSegment());
        for (String title : titles) root.addView(createSegment(title));

        return root;
    }

    private View createCurrentSegment() {
        Context context = getContext();

        LinearLayout segment = new LinearLayout(context);
        segment.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams segParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        segParams.setMargins(0, dp(8), 0, dp(16));
        segment.setLayoutParams(segParams);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(4));
        background.setStroke(dp(2), ThemeManager.divider());
        segment.setBackground(background);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(8));
        content.setGravity(Gravity.CENTER);

        boolean hasBookings = false;

        if (!hasBookings) {
            TextView emptyText = new TextView(context);
            emptyText.setText("لا يوجد حجز بعد");
            emptyText.setTypeface(ThemeManager.fontBold());
            emptyText.setTextColor(ThemeManager.textSecondary());
            emptyText.setTextSize(16);
            emptyText.setGravity(Gravity.CENTER);

            LinearLayout actionButton = new LinearLayout(context);
            actionButton.setOrientation(LinearLayout.HORIZONTAL);
            actionButton.setGravity(Gravity.CENTER);
            actionButton.setPadding(dp(16), 0, dp(16), 0);

            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setCornerRadius(dp(2));
            btnBg.setColor(ThemeManager.black());
            actionButton.setBackground(btnBg);

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48)
            );
            btnParams.topMargin = dp(12);
            btnParams.bottomMargin = dp(8);
            btnParams.gravity = Gravity.CENTER;
            actionButton.setLayoutParams(btnParams);

            TextView btnText = new TextView(context);
            btnText.setText("ابدأ الحجز الآن");
            btnText.setTextSize(14);
            btnText.setTextColor(Color.WHITE);
            btnText.setTypeface(ThemeManager.fontBold());
            btnText.setGravity(Gravity.CENTER);

            actionButton.addView(btnText);

            content.addView(emptyText);
            content.addView(actionButton);
        } else {
            TextView details = new TextView(context);
            details.setText("تفاصيل الحجوزات الحالية");
            details.setTextColor(Color.parseColor("#555555"));
            details.setTextSize(14);
            content.addView(details);
        }

        segment.addView(content);

        return segment;
    }

    private View createSegment(String title) {
        Context context = getContext();

        LinearLayout segment = new LinearLayout(context);
        segment.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams segParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        segParams.setMargins(0, dp(8), 0, dp(8));
        segment.setLayoutParams(segParams);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(4));
        background.setStroke(dp(2), ThemeManager.divider());
        segment.setBackground(background);

        TextView header = new TextView(context);
        header.setText(title);
        header.setTextColor(Color.BLACK);
        header.setTextSize(14);
        header.setTypeface(ThemeManager.fontSemiBold());
        header.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        header.setPadding(dp(8), dp(8), dp(8), dp(8));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8), dp(8), dp(8), dp(8));
        content.setVisibility(View.GONE);

        TextView details = new TextView(context);
        details.setText("تفاصيل " + title);
        details.setTextColor(Color.parseColor("#555555"));
        details.setTextSize(14);
        content.addView(details);

        header.setOnClickListener(v -> {
            boolean isOpen = content.getVisibility() == View.VISIBLE;
            content.setVisibility(isOpen ? View.GONE : View.VISIBLE);
        });

        segment.addView(header);
        segment.addView(content);

        return segment;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
