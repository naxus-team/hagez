package com.hagzy.fragments;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hagzy.MyBookingActivity;
import com.hagzy.R;
import com.hagzy.helpers.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyBookingsFragment extends Fragment {

    private LinearLayout contentLayout;
    private DatabaseReference realtimeDB;
    private FirebaseAuth mAuth;
    private ValueEventListener bookingsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        realtimeDB = FirebaseDatabase.getInstance().getReference();

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        scrollView.setBackgroundColor(Color.WHITE);
        scrollView.setFillViewport(true);

        contentLayout = new LinearLayout(getContext());
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        contentLayout.setPadding(dp(16), dp(16), dp(16), dp(16));

        scrollView.addView(contentLayout);

        loadBookings();

        return scrollView;
    }

    private void loadBookings() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            showLoginPrompt();
            return;
        }

        String userId = currentUser.getUid();

        // Show loading
        contentLayout.removeAllViews();
        contentLayout.addView(createLoadingSkeleton());

        // Listen to real-time updates
        bookingsListener = realtimeDB.child("bookings")
                .child(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        contentLayout.removeAllViews();

                        if (!snapshot.exists() || !snapshot.hasChildren()) {
                            contentLayout.addView(createEmptyState());
                            return;
                        }

                        List<BookingData> bookings = new ArrayList<>();

                        for (DataSnapshot bookingSnap : snapshot.getChildren()) {
                            BookingData booking = parseBooking(bookingSnap);
                            if (booking != null) {
                                bookings.add(booking);
                            }
                        }

                        // Sort by date (newest first)
                        Collections.sort(bookings, (b1, b2) ->
                                Long.compare(b2.bookingDate, b1.bookingDate));

                        // Group by status
                        displayBookings(bookings);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("MyBookings", "Error loading bookings", error.toException());
                        contentLayout.removeAllViews();
                        contentLayout.addView(createErrorState());
                    }
                });
    }

    private BookingData parseBooking(DataSnapshot snapshot) {
        try {
            BookingData booking = new BookingData();
            booking.bookingId = snapshot.getKey();
            booking.userId = snapshot.child("userId").getValue(String.class);
            booking.bookingName = snapshot.child("bookingName").getValue(String.class);
            booking.category = snapshot.child("category").getValue(String.class);
            booking.fieldId = snapshot.child("fieldId").getValue(String.class);

            Double price = snapshot.child("price").getValue(Double.class);
            booking.price = price != null ? price : 0.0;

            Long bookingDate = snapshot.child("bookingDate").getValue(Long.class);
            booking.bookingDate = bookingDate != null ? bookingDate : 0L;

            // Activation codes
            booking.userActivationCode = snapshot.child("userActivationCode").getValue(String.class);
            booking.providerActivationCode = snapshot.child("providerActivationCode").getValue(String.class);
            booking.activationStatus = snapshot.child("activationStatus").getValue(String.class);

            // Duration
            DataSnapshot durationSnap = snapshot.child("duration").child("startEnd");
            if (durationSnap.exists()) {
                Long start = durationSnap.child("start").getValue(Long.class);
                Long end = durationSnap.child("end").getValue(Long.class);
                booking.startTime = start != null ? start : 0L;
                booking.endTime = end != null ? end : 0L;
            }

            return booking;
        } catch (Exception e) {
            Log.e("MyBookings", "Error parsing booking", e);
            return null;
        }
    }

    private void displayBookings(List<BookingData> bookings) {
        // Separate by status
        List<BookingData> pending = new ArrayList<>();
        List<BookingData> userScanned = new ArrayList<>();
        List<BookingData> active = new ArrayList<>();
        List<BookingData> completed = new ArrayList<>();

        for (BookingData booking : bookings) {
            String status = booking.activationStatus != null ? booking.activationStatus : "pending";

            switch (status) {
                case "user_scanned":
                    userScanned.add(booking);
                    break;
                case "activated":
                    active.add(booking);
                    break;
                case "completed":
                    completed.add(booking);
                    break;
                default:
                    pending.add(booking);
                    break;
            }
        }

        // Display sections
        if (!pending.isEmpty()) {
            contentLayout.addView(createSectionTitle("⏳ في انتظار التفعيل", pending.size()));
            for (BookingData booking : pending) {
                contentLayout.addView(createBookingCard(booking));
            }
        }

        if (!userScanned.isEmpty()) {
            contentLayout.addView(createSectionTitle("🔓 في انتظار مقدم الخدمة", userScanned.size()));
            for (BookingData booking : userScanned) {
                contentLayout.addView(createBookingCard(booking));
            }
        }

        if (!active.isEmpty()) {
            contentLayout.addView(createSectionTitle("✅ الحجوزات النشطة", active.size()));
            for (BookingData booking : active) {
                contentLayout.addView(createBookingCard(booking));
            }
        }

        if (!completed.isEmpty()) {
            contentLayout.addView(createSectionTitle("📦 الحجوزات المكتملة", completed.size()));
            for (BookingData booking : completed) {
                contentLayout.addView(createBookingCard(booking));
            }
        }
    }

    private TextView createSectionTitle(String title, int count) {
        TextView section = new TextView(getContext());
        section.setText(title + " (" + count + ")");
        section.setTextSize(16);
        section.setTypeface(ThemeManager.fontBold());
        section.setTextColor(Color.parseColor("#4B463D"));
        section.setTranslationY(-dpf(1.5f));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.topMargin = dp(8);
        params.bottomMargin = dp(12);
        section.setLayoutParams(params);

        return section;
    }

    private CardView createBookingCard(BookingData booking) {
        CardView card = new CardView(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(16));
        card.setCardElevation(0);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(2), Color.parseColor("#EFEDE9"));
        bg.setColor(Color.WHITE);
        card.setBackground(bg);

        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Header row
        LinearLayout headerRow = new LinearLayout(getContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        headerParams.bottomMargin = dp(12);
        headerRow.setLayoutParams(headerParams);

        TextView titleText = new TextView(getContext());
        titleText.setText(booking.bookingName != null ? booking.bookingName : "حجز");
        titleText.setTextSize(18);
        titleText.setTypeface(ThemeManager.fontBold());
        titleText.setTextColor(Color.parseColor("#4B463D"));
        titleText.setTranslationY(-dpf(1.5f));
        titleText.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));

        headerRow.addView(titleText);
        headerRow.addView(createStatusBadge(booking.activationStatus));

        content.addView(headerRow);

        // Details box
        LinearLayout detailsBox = new LinearLayout(getContext());
        detailsBox.setOrientation(LinearLayout.VERTICAL);
        detailsBox.setPadding(dp(12), dp(12), dp(12), dp(12));

        GradientDrawable detailsBg = new GradientDrawable();
        detailsBg.setCornerRadius(dp(12));
        detailsBg.setColor(Color.parseColor("#F8F8F8"));
        detailsBox.setBackground(detailsBg);

        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        detailsParams.bottomMargin = dp(12);
        detailsBox.setLayoutParams(detailsParams);

        if (booking.category != null && !booking.category.isEmpty()) {
            detailsBox.addView(createDetailRow("📋 النوع", booking.category));
        }

        detailsBox.addView(createDetailRow("💰 السعر",
                String.format(Locale.getDefault(), "%.0f ج.م", booking.price)));

        if (booking.startTime > 0 && booking.endTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM, hh:mm a", new Locale("ar"));
            detailsBox.addView(createDetailRow("🕐 من", sdf.format(new Date(booking.startTime))));
            detailsBox.addView(createDetailRow("🕐 إلى", sdf.format(new Date(booking.endTime))));
        }

        content.addView(detailsBox);

        // Action button
        LinearLayout actionBtn = createActionButton(booking);
        if (actionBtn != null) {
            content.addView(actionBtn);
        }

        card.addView(content);

        // Click to view details
        card.setOnClickListener(v -> openBookingDetails(booking));

        return card;
    }

    private LinearLayout createStatusBadge(String status) {
        LinearLayout badge = new LinearLayout(getContext());
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(10), dp(5), dp(10), dp(5));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));

        String text;
        int bgColor;
        int textColor;

        if (status == null) status = "pending";

        switch (status) {
            case "user_scanned":
                text = "في الانتظار";
                bgColor = Color.parseColor("#FFF3E0");
                textColor = Color.parseColor("#F57C00");
                break;
            case "activated":
                text = "نشط";
                bgColor = Color.parseColor("#E8F5E9");
                textColor = Color.parseColor("#2E7D32");
                break;
            case "completed":
                text = "مكتمل";
                bgColor = Color.parseColor("#F5F5F5");
                textColor = Color.parseColor("#757575");
                break;
            default:
                text = "قيد التفعيل";
                bgColor = Color.parseColor("#E3F2FD");
                textColor = Color.parseColor("#1976D2");
                break;
        }

        bg.setColor(bgColor);
        badge.setBackground(bg);

        TextView badgeText = new TextView(getContext());
        badgeText.setText(text);
        badgeText.setTextSize(12);
        badgeText.setTypeface(ThemeManager.fontBold());
        badgeText.setTextColor(textColor);
        badgeText.setTranslationY(-dpf(1f));

        badge.addView(badgeText);
        return badge;
    }

    private LinearLayout createDetailRow(String label, String value) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView labelText = new TextView(getContext());
        labelText.setText(label);
        labelText.setTextSize(14);
        labelText.setTypeface(ThemeManager.fontSemiBold());
        labelText.setTextColor(Color.parseColor("#804B463D"));
        labelText.setTranslationY(-dpf(1.5f));
        labelText.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));

        TextView valueText = new TextView(getContext());
        valueText.setText(value);
        valueText.setTextSize(14);
        valueText.setTypeface(ThemeManager.fontBold());
        valueText.setTextColor(Color.parseColor("#4B463D"));
        valueText.setGravity(Gravity.END);
        valueText.setTranslationY(-dpf(1.5f));
        valueText.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f));

        row.addView(labelText);
        row.addView(valueText);

        return row;
    }

    private LinearLayout createActionButton(BookingData booking) {
        String status = booking.activationStatus != null ? booking.activationStatus : "pending";

        LinearLayout btn = new LinearLayout(getContext());
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, dp(12), 0, dp(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));

        TextView btnText = new TextView(getContext());
        btnText.setTextSize(15);
        btnText.setTypeface(ThemeManager.fontBold());
        btnText.setTranslationY(-dpf(1.5f));

        switch (status) {
            case "pending":
                bg.setColor(Color.parseColor("#2196F3"));
                btnText.setText("عرض التفاصيل");
                btnText.setTextColor(Color.WHITE);
                break;
            case "user_scanned":
                bg.setColor(Color.parseColor("#FF9800"));
                btnText.setText("في انتظار التفعيل");
                btnText.setTextColor(Color.WHITE);
                break;
            case "activated":
                bg.setColor(Color.parseColor("#4CAF50"));
                btnText.setText("استخدام الحجز");
                btnText.setTextColor(Color.WHITE);
                break;
            case "completed":
                bg.setColor(Color.parseColor("#E0E0E0"));
                btnText.setText("عرض التفاصيل");
                btnText.setTextColor(Color.parseColor("#757575"));
                break;
            default:
                return null;
        }

        btn.setBackground(bg);
        btn.addView(btnText);

        btn.setOnClickListener(v -> openBookingDetails(booking));

        return btn;
    }

    private void openBookingDetails(BookingData booking) {
        Intent intent = new Intent(getContext(), MyBookingActivity.class);
        intent.putExtra("bookingId", booking.bookingId);
        intent.putExtra("userId", booking.userId);
        startActivity(intent);
    }

    private LinearLayout createLoadingSkeleton() {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dp(16), 0, dp(16));

        for (int i = 0; i < 3; i++) {
            View skeleton = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, dp(120));
            params.bottomMargin = dp(12);
            skeleton.setLayoutParams(params);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(Color.parseColor("#F0F0F0"));
            skeleton.setBackground(bg);

            container.addView(skeleton);
        }

        return container;
    }

    private LinearLayout createEmptyState() {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dp(32), dp(64), dp(32), dp(64));

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(R.drawable.check_badge);
        icon.setColorFilter(Color.parseColor("#C0BBB3"));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(80), dp(80));
        iconParams.bottomMargin = dp(16);
        icon.setLayoutParams(iconParams);

        TextView emptyText = new TextView(getContext());
        emptyText.setText("لا توجد حجوزات حالياً");
        emptyText.setTextSize(18);
        emptyText.setTypeface(ThemeManager.fontBold());
        emptyText.setTextColor(Color.parseColor("#804B463D"));
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setTranslationY(-dpf(1.5f));

        TextView emptySubtext = new TextView(getContext());
        emptySubtext.setText("ابدأ بحجز ملعبك المفضل!");
        emptySubtext.setTextSize(14);
        emptySubtext.setTypeface(ThemeManager.fontSemiBold());
        emptySubtext.setTextColor(Color.parseColor("#60000000"));
        emptySubtext.setGravity(Gravity.CENTER);
        emptySubtext.setTranslationY(-dpf(1f));
        LinearLayout.LayoutParams subtextParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        subtextParams.topMargin = dp(8);
        emptySubtext.setLayoutParams(subtextParams);

        container.addView(icon);
        container.addView(emptyText);
        container.addView(emptySubtext);

        return container;
    }

    private LinearLayout createLoginPrompt() {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dp(32), dp(64), dp(32), dp(64));

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(R.drawable.check_badge);
        icon.setColorFilter(Color.parseColor("#C0BBB3"));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(80), dp(80));
        iconParams.bottomMargin = dp(16);
        icon.setLayoutParams(iconParams);

        TextView text = new TextView(getContext());
        text.setText("يجب تسجيل الدخول\nلعرض حجوزاتك");
        text.setTextSize(18);
        text.setTypeface(ThemeManager.fontBold());
        text.setTextColor(Color.parseColor("#804B463D"));
        text.setGravity(Gravity.CENTER);
        text.setTranslationY(-dpf(1.5f));
        text.setLineSpacing(dp(4), 1.0f);

        container.addView(icon);
        container.addView(text);

        return container;
    }

    private void showLoginPrompt() {
        contentLayout.removeAllViews();
        contentLayout.addView(createLoginPrompt());
    }

    private LinearLayout createErrorState() {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dp(32), dp(64), dp(32), dp(64));

        TextView text = new TextView(getContext());
        text.setText("❌ حدث خطأ في تحميل الحجوزات");
        text.setTextSize(16);
        text.setTypeface(ThemeManager.fontBold());
        text.setTextColor(Color.parseColor("#F44336"));
        text.setGravity(Gravity.CENTER);
        text.setTranslationY(-dpf(1.5f));

        container.addView(text);

        return container;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Remove listener to prevent memory leaks
        if (bookingsListener != null && mAuth.getCurrentUser() != null) {
            realtimeDB.child("bookings")
                    .child(mAuth.getCurrentUser().getUid())
                    .removeEventListener(bookingsListener);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private float dpf(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    // Data model class
    private static class BookingData {
        String bookingId;
        String userId;
        String bookingName;
        String category;
        String fieldId;
        double price;
        long bookingDate;
        long startTime;
        long endTime;
        String userActivationCode;
        String providerActivationCode;
        String activationStatus;
    }
}